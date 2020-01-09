package cn.keking.project.binlogdistributor.app.service.oplog;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.service.EtcdService;
import cn.keking.project.binlogdistributor.app.util.EtcdKeyPrefixUtil;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import org.bson.BsonTimestamp;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author: kl @kailing.pub
 * @date: 2020/1/7
 */
@Component
public class OpLogClientFactory {

    private static final Logger log = LoggerFactory.getLogger(OpLogClientFactory.class);

    protected final RedissonClient redissonClient;

    private final DataPublisher dataPublisher;

    private final EtcdService etcdService;

    private final Client etcdClient;

    private final BinaryLogConfigContainer binaryLogConfigContainer;

    private final EtcdKeyPrefixUtil etcdKeyPrefixUtil;

    private volatile Set<String> activeNameSpaces = ConcurrentHashMap.newKeySet();

    public volatile AtomicLong eventCount = new AtomicLong(0);

    /**
     * 事件类型的key，值可为i(插入)、u(更新)、d(删除)
     */
    public static final String EVENTTYPE_KEY = "op";
    /**
     * 数据库、集合名称
     */
    public static final String DATABASE_KEY = "ns";
    /**
     * 消费时间戳
     */
    public static final String TIMESTAMP_KEY = "ts";
    /**
     * 内容
     */
    public static final String CONTEXT_KEY = "o";
    /**
     * 更新事件更新的条件
     */
    public static final String UPDATE_WHERE_KEY = "o2";
    /**
     * 更新事件，更新的内容
     */
    public static final String UPDATE_CONTEXT_KEY = "$set";


    private long lastEventCount = 0;

    public OpLogClientFactory(RedissonClient redissonClient, @Qualifier("opLogDataPublisher") DataPublisher dataPublisher, EtcdService etcdService, Client etcdClient, BinaryLogConfigContainer binaryLogConfigContainer, EtcdKeyPrefixUtil etcdKeyPrefixUtil) {
        this.redissonClient = redissonClient;
        this.dataPublisher = dataPublisher;
        this.etcdService = etcdService;
        this.etcdClient = etcdClient;
        this.binaryLogConfigContainer = binaryLogConfigContainer;
        this.etcdKeyPrefixUtil = etcdKeyPrefixUtil;
    }

    public OplogClient initClient(BinaryLogConfig binaryLogConfig) {

        String namespace = binaryLogConfig.getNamespace();

        MongoClient mongoClient = this.getMongoClient(binaryLogConfig);
        OpLogEventContext context = new OpLogEventContext(mongoClient, etcdService, binaryLogConfig, dataPublisher);

        OpLogEventHandlerFactory opLogEventHandlerFactory = new OpLogEventHandlerFactory(context);

        // 初始化用户关注列表
        initClients(binaryLogConfig, opLogEventHandlerFactory);

        OplogClient client = new OplogClient(mongoClient, opLogEventHandlerFactory);
        // 配置当前位置
        configOpLogStatus(client, binaryLogConfig);
        // 启动Client列表数据监听
        registerMetaDataWatcher(binaryLogConfig, opLogEventHandlerFactory);
        // 记录active namespaces
        activeNameSpaces.add(namespace);
        return client;
    }

    /**
     * 初始化列表
     *
     * @param binaryLogConfig
     * @param opLogEventHandlerFactory
     */
    private void initClients(BinaryLogConfig binaryLogConfig, OpLogEventHandlerFactory opLogEventHandlerFactory) {

        List<ClientInfo> clientSet = etcdService.listBinLogConsumerClient(binaryLogConfig);
        clientSet.forEach(opLogEventHandlerFactory::addClientLocal);
    }

    /**
     * 配置当前binlog位置
     *
     * @param oplogClient
     * @param binaryLogConfig
     */
    private void configOpLogStatus(OplogClient oplogClient, BinaryLogConfig binaryLogConfig) {
        JSONObject binLogStatus = etcdService.getBinaryLogStatus(binaryLogConfig);
        if (binLogStatus != null) {
            int seconds = binLogStatus.getIntValue("binlogFilename");
            int inc = binLogStatus.getIntValue("binlogPosition");
            oplogClient.setTs(new BsonTimestamp(seconds, inc));
        }
    }

    private MongoClient getMongoClient(BinaryLogConfig binaryLogConfig) {
        return new MongoClient(new MongoClientURI(binaryLogConfig.getDataSourceUrl()));
    }

    /**
     * 注册Client列表更新监听
     *
     * @param binaryLogConfig
     * @param opLogEventHandlerFactory
     */
    private void registerMetaDataWatcher(BinaryLogConfig binaryLogConfig, OpLogEventHandlerFactory opLogEventHandlerFactory) {

        String namespace = binaryLogConfig.getNamespace();
        String binLogClientSet = binaryLogConfig.getBinLogClientSet();

        Watch watchClient = etcdClient.getWatchClient();
        watchClient.watch(
                ByteSequence.from(etcdKeyPrefixUtil.withPrefix(namespace).concat(Constants.PATH_SEPARATOR).concat(binLogClientSet), StandardCharsets.UTF_8),
                WatchOption.newBuilder().withNoDelete(true).build(),
                new Watch.Listener() {
                    @Override
                    public void onNext(WatchResponse response) {
                        List<WatchEvent> eventList = response.getEvents();
                        for (WatchEvent event : eventList) {
                            if (WatchEvent.EventType.PUT.equals(event.getEventType())) {

                                KeyValue currentKV = event.getKeyValue();
                                Set<ClientInfo> currentClientInfoSet = getClientInfos(currentKV);

                                currentClientInfoSet
                                        .stream()
                                        .collect(Collectors.groupingBy(ClientInfo::getDatabaseEvent))
                                        .forEach(opLogEventHandlerFactory::updateClientBatch);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error("Watch clientInfo list change error.", throwable);
                    }

                    @Override
                    public void onCompleted() {
                        log.info("Watch clientInfo list change completed.");
                    }
                }
        );

    }

    private Set<ClientInfo> getClientInfos(KeyValue keyValue) {
        Set<ClientInfo> clientInfos;
        if (keyValue == null || keyValue.getValue() == null) {
            clientInfos = new HashSet<>();
        } else {
            clientInfos = new HashSet<>(JSON.parseArray(keyValue.getValue().toString(StandardCharsets.UTF_8), ClientInfo.class));
        }
        return clientInfos;
    }

    public boolean closeClient(OplogClient oplogClient, String namespace) {
        try {
            // remove active namespace
            activeNameSpaces.remove(namespace);
            oplogClient.close();
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        BinaryLogConfig binaryLogConfig = binaryLogConfigContainer.getConfigByNamespace(namespace);
        try {
            while (binaryLogConfig.isActive()) {
                TimeUnit.SECONDS.sleep(1);
                binaryLogConfig = binaryLogConfigContainer.getConfigByNamespace(namespace);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public Set<String> getActiveNameSpaces() {
        return activeNameSpaces;
    }

    public long getEventCount() {
        return eventCount.get();
    }

    public long eventCountSinceLastTime() {

        long total = eventCount.get();
        long res = total - lastEventCount;

        lastEventCount = total;

        return res;
    }
}
