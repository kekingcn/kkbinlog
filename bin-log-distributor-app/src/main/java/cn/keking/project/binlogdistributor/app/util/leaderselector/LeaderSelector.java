package cn.keking.project.binlogdistributor.app.util.leaderselector;

import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lock.LockResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wanglaomo
 * @since 2019/7/29
 **/
public class LeaderSelector implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LeaderSelector.class);

    private static final String DEFAULT_LEADER_IDENTIFICATION_PATH = "/leader-selector/leader-identification/";

    /**
     * etcd client
     */
    private final Client client;

    /**
     * invoke when acquire leadership
     */
    private final LeaderSelectorListener leaderSelectorListener;

    /**
     * etcd lock client
     */
    private final Lock lockClient;

    /**
     * etcd lease client
     */
    private final Lease leaseClient;

    private CloseableClient leaseCloser;

    /**
     * etcd kv client
     */
    private final KV kvClient;

    /**
     * leader selector state
     */
    private final AtomicReference<State> state = new AtomicReference<>(State.STARTED);

    /**
     * keep racing for leadership when losing
     */
    private boolean autoRequeue;

    /**
     * the path for this leadership group
     */
    private final String leaderPath;

    /**
     * the path for this leadership identification
     */
    private final String leaderIdentificationPath;

    private final ByteSequence identification;

    private final long leaseTTL;
    private enum State {
        //持有领导权
        HOLD,
        //准备领导竞争
        STARTED,
        //关闭
        CLOSE
    }

    public LeaderSelector(Client client, String leaderPath, Long leaseTTL, String identification, LeaderSelectorListener listener) {
        this(client, leaderPath, leaseTTL, identification, DEFAULT_LEADER_IDENTIFICATION_PATH,true, listener);
    }

    public LeaderSelector(Client client, String leaderPath, Long leaseTTL, String identification, String leaderIdentificationPath, LeaderSelectorListener listener) {
        this(client, leaderPath, leaseTTL, identification, leaderIdentificationPath, true, listener);
    }

    public LeaderSelector (Client client, String leaderPath, Long leaseTTL, String identification, String leaderIdentificationPath, boolean autoRequeue, LeaderSelectorListener listener) {
        this.client = client;
        this.lockClient = client.getLockClient();
        this.leaseClient = client.getLeaseClient();
        this.kvClient = client.getKVClient();
        this.leaderPath = leaderPath;
        this.leaseTTL = leaseTTL;
        this.autoRequeue = autoRequeue;
        this.leaderSelectorListener = listener;
        this.identification = identification == null ? null : ByteSequence.from(identification, StandardCharsets.UTF_8);
        leaderIdentificationPath = StringUtils.isEmpty(leaderIdentificationPath) ? DEFAULT_LEADER_IDENTIFICATION_PATH : leaderIdentificationPath;
        if(!leaderIdentificationPath.endsWith("/")) {
            leaderIdentificationPath = leaderIdentificationPath.concat("/");
        }
        this.leaderIdentificationPath = leaderIdentificationPath;
    }

    /**
     * get current leader.
     *
     * @return null if there is no current leader.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public String getLeader() throws ExecutionException, InterruptedException {

        KeyValue identificationKV = kvClient
                .get(ByteSequence.from(leaderIdentificationPath.concat(leaderPath), StandardCharsets.UTF_8))
                .get()
                // null safety
                .getKvs()
                .get(0);

        return identificationKV == null ? null : identificationKV.getValue().toString(StandardCharsets.UTF_8);
    }

    /**
     * start racing for leadership.
     */
    public void start() {
        logger.info("LeaderSelector start racing for leadership");
        leaderElection();
    }


    private synchronized void leaderElection() {
        logger.debug("LeaderSelector start");
        try {
            doWorkInternal();
        } catch (Exception e) {
            logger.error("LeaderSelector has some error.", e);
            cancelTask();
        }
    }

    private void doWorkInternal() throws Exception {

        long leaseId = acquireActiveLease();
        // acquire distributed lock
        LockResponse lockResponse = lockClient.lock(ByteSequence.from(leaderPath, StandardCharsets.UTF_8), leaseId).get();
        logger.debug("LeaderSelector successfully get Lock [{}]", lockResponse.getKey().toString(StandardCharsets.UTF_8));
        if(!state.get().equals(State.CLOSE)){
            // update etcd leader identification to support query
            ByteSequence leaderIdentificationPathByte = ByteSequence.from(leaderIdentificationPath.concat(leaderPath), StandardCharsets.UTF_8);
            ByteSequence finalIdentification = Optional.ofNullable(identification).orElse(ByteSequence.from(String.valueOf(UUID.randomUUID()), StandardCharsets.UTF_8));
            logger.info("LeaderSelector uses [{}] as identification", finalIdentification.toString(StandardCharsets.UTF_8));
            kvClient.put(leaderIdentificationPathByte, finalIdentification).get();
            Assert.isTrue(state.compareAndSet(State.STARTED,State.HOLD),"Leadership cannot be acquired without initiating the process");
            leaderSelectorListener.afterTakeLeadership();
        }
    }

    private void cancelTask() {
        try {
            if (state.equals(State.HOLD) && leaderSelectorListener.afterLosingLeadership()) {
                state.set(State.STARTED);
            }
            if (leaseCloser != null) {
                leaseCloser.close();
            }
        } catch (Exception e) {
            state.set(State.STARTED);
        }
        logger.debug("LeaderSelector has successfully canceled the task.");
        if(autoRequeue) {
                try {
                    // 延迟领导选举
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                leaderElection();
        }
    }

    /**
     * acquire etcd lease and keep lease avtive
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private long acquireActiveLease() throws InterruptedException, ExecutionException {
        long leaseId = leaseClient.grant(leaseTTL).get().getID();
        logger.debug("LeaderSelector get leaseId:[{}] and ttl:[{}]", leaseId, leaseTTL);
        this.leaseCloser = leaseClient.keepAlive(leaseId, new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse value) {
                logger.debug("LeaderSelector lease keeps alive for [{}]s:", value.getTTL());
            }

            @Override
            public void onError(Throwable t) {
                logger.debug("LeaderSelector lease renewal Exception!", t.fillInStackTrace());
                cancelTask();
            }

            @Override
            public void onCompleted() {
                logger.info("LeaderSelector lease renewal completed! start canceling task.");
                cancelTask();
            }
        });
        return leaseId;
    }

    @Override
    public void close() {
        if(State.HOLD.equals(state.get())){
            if(leaderSelectorListener.afterLosingLeadership()) {
                state.compareAndSet(State.HOLD, State.STARTED);
            }
        }
        state.set(State.CLOSE);
        leaseCloser.close();
        logger.info("LeaderSelector has been closed");
    }
}
