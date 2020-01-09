package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.exception.EtcdServerException;
import cn.keking.project.binlogdistributor.app.model.ServiceStatus;
import cn.keking.project.binlogdistributor.app.service.DistributorServiceAbstract;
import cn.keking.project.binlogdistributor.app.util.EtcdKeyPrefixUtil;
import cn.keking.project.binlogdistributor.app.util.NetUtils;
import cn.keking.project.binlogdistributor.app.util.leaderselector.BinlogLeaderSelectorListener;
import cn.keking.project.binlogdistributor.app.util.leaderselector.LeaderSelector;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static cn.keking.project.binlogdistributor.app.service.binlog.MysqlDistributorServiceImpl.TYPE;

/**
 * @author wanglaomo
 * @since 2019/6/10
 **/
@Service(value = TYPE)
@Qualifier
public class MysqlDistributorServiceImpl extends DistributorServiceAbstract {

    public static final String TYPE = "MySQL";

    private final BinLogClientFactory binLogClientFactory;

    private final Client etcdClient;

    private final EtcdKeyPrefixUtil etcdKeyPrefixUtil;

    protected final DataPublisher dataPublisher;

    private ExecutorService executorService;

    private Map<String, LeaderSelector> leaderSelectorMap = new ConcurrentHashMap<>();

    public MysqlDistributorServiceImpl(BinLogClientFactory binLogClientFactory, Client etcdClient, EtcdKeyPrefixUtil etcdKeyPrefixUtil, @Qualifier("binlogDataPublisher")DataPublisher dataPublisher) {
        this.binLogClientFactory = binLogClientFactory;
        this.etcdClient = etcdClient;
        this.etcdKeyPrefixUtil = etcdKeyPrefixUtil;
        this.dataPublisher = dataPublisher;
    }

    /**
     * 开始分发binlog
     */
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
        // 3. 服务节点上报
        updateServiceStatus(TYPE);

    }

    @Override
    public void submitBinLogDistributeTask(BinaryLogConfig config) {
        executorService.submit(() -> binLogDistributeTask(config));
    }

    @Override
    public void stopBinLogDistributeTask(String namespace) {
        LeaderSelector leaderSelector = leaderSelectorMap.get(namespace);
        if(leaderSelector != null) {
            leaderSelector.close();
            leaderSelectorMap.remove(namespace);
        }
    }

    private void binLogDistributeTask(BinaryLogConfig binaryLogConfig) {

        String namespace = binaryLogConfig.getNamespace();
        String identification = NetUtils.getLocalAddress().getHostAddress();
        String identificationPath = etcdKeyPrefixUtil.withPrefix(Constants.LEADER_IDENTIFICATION_PATH);
        BinlogLeaderSelectorListener listener = new BinlogLeaderSelectorListener(binLogClientFactory,binaryLogConfig);
        LeaderSelector leaderSelector = new LeaderSelector(etcdClient, binaryLogConfig.getNamespace(), 20L, identification, identificationPath, listener);
        leaderSelectorMap.put(namespace, leaderSelector);
        leaderSelector.start();
    }

    protected void updateServiceStatus(String dataSourceType) {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        try {
            long leaseId = etcdService.getLease("Update Service Status", 20);
            scheduledExecutorService.scheduleWithFixedDelay(() -> {
                ServiceStatus serviceStatus = new ServiceStatus();
                String localIp = BinaryLogConfigContainer.getLocalIp(dataSourceType);
                serviceStatus.setIp(localIp);
                serviceStatus.setActiveNamespaces(binLogClientFactory.getActiveNameSpaces());
                serviceStatus.setTotalEventCount(binLogClientFactory.getEventCount());
                serviceStatus.setLatelyEventCount(binLogClientFactory.eventCountSinceLastTime());
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
