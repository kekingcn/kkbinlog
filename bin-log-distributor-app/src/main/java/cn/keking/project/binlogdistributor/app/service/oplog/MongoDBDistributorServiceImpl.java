package cn.keking.project.binlogdistributor.app.service.oplog;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.exception.EtcdServerException;
import cn.keking.project.binlogdistributor.app.model.ServiceStatus;
import cn.keking.project.binlogdistributor.app.service.DistributorServiceAbstract;
import cn.keking.project.binlogdistributor.app.service.EtcdService;
import cn.keking.project.binlogdistributor.app.util.EtcdKeyPrefixUtil;
import cn.keking.project.binlogdistributor.app.util.NetUtils;
import cn.keking.project.binlogdistributor.app.util.leaderselector.LeaderSelector;
import cn.keking.project.binlogdistributor.app.util.leaderselector.OplogLeaderSelectorListener;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static cn.keking.project.binlogdistributor.app.service.oplog.MongoDBDistributorServiceImpl.TYPE;

/**
 * @author: kl @kailing.pub
 * @date: 2020/1/7
 */
@Service(value = TYPE)
public class MongoDBDistributorServiceImpl extends DistributorServiceAbstract {

    public static final String TYPE = "MongoDB";

    private final BinaryLogConfigContainer binaryLogConfigContainer;

    private final OpLogClientFactory opLogClientFactory;

    private final EtcdService etcdService;

    private final Client etcdClient;

    private final EtcdKeyPrefixUtil etcdKeyPrefixUtil;

    private final DataPublisher dataPublisher;

    private ExecutorService executorService;

    private ScheduledExecutorService scheduledExecutorService;

    private Map<String, LeaderSelector> leaderSelectorMap = new ConcurrentHashMap<>();

    public MongoDBDistributorServiceImpl(OpLogClientFactory opLogClientFactory, BinaryLogConfigContainer binaryLogConfigContainer, EtcdService etcdService, Client etcdClient, EtcdKeyPrefixUtil etcdKeyPrefixUtil,@Qualifier("opLogDataPublisher") DataPublisher dataPublisher) {
        this.opLogClientFactory = opLogClientFactory;
        this.binaryLogConfigContainer = binaryLogConfigContainer;
        this.etcdService = etcdService;
        this.etcdClient = etcdClient;
        this.etcdKeyPrefixUtil = etcdKeyPrefixUtil;
        this.dataPublisher = dataPublisher;
    }

    @Override
    public void startDistribute() {
        // 1. 从etcd获得初始配置信息
        List<BinaryLogConfig> configList = binaryLogConfigContainer.initAllConfigs(TYPE);

        // 2. 竞争每个数据源的Leader
        executorService = Executors.newCachedThreadPool();
        configList.forEach( config -> {
            // 在线程中启动事件监听
            if(config.isActive()) {
                submitBinLogDistributeTask(config);
            }
        });

        // 3. 注册数据源Config 命令Watcher
        binaryLogConfigContainer.registerConfigCommandWatcher();

        // 4. 服务节点上报
        updateServiceStatus(TYPE);
    }



    @Override
    public void submitBinLogDistributeTask(BinaryLogConfig config) {
        executorService.submit(() -> binLogDistributeTask(config));
    }

    private void binLogDistributeTask(BinaryLogConfig binaryLogConfig) {
        String namespace = binaryLogConfig.getNamespace();
        String identification = NetUtils.getLocalAddress().getHostAddress();
        String identificationPath = etcdKeyPrefixUtil.withPrefix(Constants.LEADER_IDENTIFICATION_PATH);
        OplogLeaderSelectorListener listener = new OplogLeaderSelectorListener(opLogClientFactory,binaryLogConfig,binaryLogConfigContainer);
        LeaderSelector leaderSelector = new LeaderSelector(etcdClient, binaryLogConfig.getNamespace(), 20L, identification, identificationPath, listener);
        leaderSelectorMap.put(namespace, leaderSelector);
        leaderSelector.start();
    }

    @Override
    public void stopBinLogDistributeTask(String namespace) {
        LeaderSelector leaderSelector = leaderSelectorMap.get(namespace);
        if(leaderSelector != null) {
            leaderSelector.close();
        }
    }


    protected void updateServiceStatus(String dataSourceType) {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        try {
            long leaseId = etcdService.getLease("Update Service Status", 20);
            scheduledExecutorService.scheduleWithFixedDelay(() -> {

                ServiceStatus serviceStatus = new ServiceStatus();
                String localIp = BinaryLogConfigContainer.getLocalIp(dataSourceType);
                serviceStatus.setIp(localIp);
                serviceStatus.setActiveNamespaces(opLogClientFactory.getActiveNameSpaces());
                serviceStatus.setTotalEventCount(opLogClientFactory.getEventCount());
                serviceStatus.setLatelyEventCount(opLogClientFactory.eventCountSinceLastTime());
                serviceStatus.setTotalPublishCount(dataPublisher.getPublishCount());
                serviceStatus.setLatelyPublishCount(dataPublisher.publishCountSinceLastTime());
                serviceStatus.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

                try {
                    etcdService.updateServiceStatus(localIp, serviceStatus, leaseId);
                    logger.info("Update Service Status: [{}]", serviceStatus.toString());
                } catch (Exception e) {
                    throw new EtcdServerException("Update Service Status Error!", e);
                }
            },10, 20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new EtcdServerException("Update Service Status Error!", e);
        }
    }
}
