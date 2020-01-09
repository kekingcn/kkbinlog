package cn.keking.project.binlogdistributor.app.config;

import cn.keking.project.binlogdistributor.app.exception.EtcdServerException;
import cn.keking.project.binlogdistributor.app.model.BinLogCommand;
import cn.keking.project.binlogdistributor.app.model.enums.BinLogCommandType;
import cn.keking.project.binlogdistributor.app.service.DistributorService;
import cn.keking.project.binlogdistributor.app.util.EtcdKeyPrefixUtil;
import cn.keking.project.binlogdistributor.app.util.NetUtils;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import com.alibaba.fastjson.JSON;
import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author wanglaomo
 * @since 2019/6/4
 **/
@Component
public class BinaryLogConfigContainer {

    private static final Logger logger = LoggerFactory.getLogger(BinaryLogConfigContainer.class);

    private final Client etcdClient;

    private final EtcdKeyPrefixUtil etcdKeyPrefixUtil;

    private final ApplicationContext applicationContext;

    public BinaryLogConfigContainer(Client etcdClient, EtcdKeyPrefixUtil etcdKeyPrefixUtil, ApplicationContext applicationContext) {
        this.etcdClient = etcdClient;
        this.etcdKeyPrefixUtil = etcdKeyPrefixUtil;
        this.applicationContext = applicationContext;
    }

    public List<BinaryLogConfig> initAllConfigs(String dataSourceType) {

        List<BinaryLogConfig> binaryLogConfigs = getConfigs();

        if(binaryLogConfigs.isEmpty()) {
            logger.warn("There is no available binlog config!");
            return binaryLogConfigs;
        }
        Set<String> namespaces = new HashSet<>();
        List<BinaryLogConfig> filterDataSource = new ArrayList<>();
        binaryLogConfigs.forEach(config -> {
            if(StringUtils.isEmpty(config.getNamespace())) {
                throw new IllegalArgumentException("You need to config namespace!");
            }
            if(!namespaces.add(config.getNamespace())) {
                throw new IllegalArgumentException("Duplicated namespace!");
            }
            if(config.getDataSourceType().equals(dataSourceType)){
                filterDataSource.add(config);
            }
        });
       return filterDataSource;
    }

    public boolean addConfig(BinaryLogConfig newConfig) {

        List<BinaryLogConfig> binaryLogConfigs = getConfigs();

        boolean exist = binaryLogConfigs.stream().anyMatch(c -> c.getNamespace().equals(newConfig.getNamespace()));

        if(exist) {
            return false;
        }

        binaryLogConfigs.add(newConfig);
        persistConfig(binaryLogConfigs);

        return true;
    }

    public BinaryLogConfig removeConfig(String namespace) {

        if(StringUtils.isEmpty(namespace)) {
            return null;
        }

        BinaryLogConfig removedConfig = null;

        List<BinaryLogConfig> binaryLogConfigs = getConfigs();
        Iterator<BinaryLogConfig> iterator = binaryLogConfigs.iterator();
        while (iterator.hasNext()){
            BinaryLogConfig config = iterator.next();
            if(config.getNamespace().equals(namespace)) {
                removedConfig = config;
                iterator.remove();
                break;
            }
        }

        persistConfig(binaryLogConfigs);

        return removedConfig;
    }

    public BinaryLogConfig getConfigByNamespace(String namespace) {

        List<BinaryLogConfig> binaryLogConfigs = getConfigs();
        Optional<BinaryLogConfig> optional = binaryLogConfigs.stream().filter(config -> namespace.equals(config.getNamespace())).findAny();

        if (optional.isPresent()) {
            return optional.get();
        }

        return null;
    }

    public List<BinaryLogConfig> getConfigs() {

        KV kvClient = etcdClient.getKVClient();
        List<BinaryLogConfig> binaryLogConfigs = new ArrayList<>();
        try {
            GetResponse configRes = kvClient.get(ByteSequence.from(etcdKeyPrefixUtil.withPrefix(Constants.DEFAULT_BINLOG_CONFIG_KEY), StandardCharsets.UTF_8)).get();

            if(configRes == null || configRes.getCount() == 0) {
                return binaryLogConfigs;
            }
            // not range query
            String configListStr = configRes.getKvs().get(0).getValue().toString(StandardCharsets.UTF_8);
            binaryLogConfigs = JSON.parseArray(configListStr, BinaryLogConfig.class);
        } catch (InterruptedException | ExecutionException e) {
            throw new EtcdServerException("Failed to connect to etcd server.", e);
        }
        return binaryLogConfigs;
    }

    public void registerConfigCommandWatcher() {

        Watch watchClient = etcdClient.getWatchClient();
        watchClient.watch(
                ByteSequence.from(etcdKeyPrefixUtil.withPrefix(Constants.DEFAULT_BINLOG_CONFIG_COMMAND_KEY), StandardCharsets.UTF_8),
                WatchOption.newBuilder().withPrevKV(true).withNoDelete(true).build(),
                new Watch.Listener() {

                    @Override
                    public void onNext(WatchResponse response) {

                        List<WatchEvent> eventList = response.getEvents();
                        for(WatchEvent event: eventList) {

                            if (WatchEvent.EventType.PUT.equals(event.getEventType())) {
                                BinLogCommand command = JSON.parseObject(event.getKeyValue().getValue().toString(StandardCharsets.UTF_8), BinLogCommand.class);

                                // 根据不同的命令类型（START/STOP）执行不同的逻辑
                                if(BinLogCommandType.START_DATASOURCE.equals(command.getType())) {
                                    handleStartDatasource(command.getNamespace(), command.getDelegatedIp());
                                } else if (BinLogCommandType.STOP_DATASOURCE.equals(command.getType())) {
                                    handleStopDatasource(command.getNamespace());
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Watch binlog config command error.", throwable);
                        new Thread(() -> registerConfigCommandWatcher()).start();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Watch binlog config command completed.");
                        new Thread(() -> registerConfigCommandWatcher()).start();
                    }
                }
        );
    }

    /**
     * 真正开启数据源的逻辑
     *
     * @param namespace
     * @param delegatedIp
     * @return
     */
    public void handleStartDatasource(String namespace, String delegatedIp) {
        if(StringUtils.isEmpty(namespace)) {
            return;
        }

        if(!StringUtils.isEmpty(delegatedIp)) {
            BinaryLogConfig config = this.getConfigByNamespace(namespace);
            String localIp = getLocalIp(config.getDataSourceType());
            if(!delegatedIp.equals(localIp)) {
                logger.info("Ignore start database command for ip not matching. local: [{}] delegatedId: [{}]", localIp, delegatedIp);
                try {
                    // 非指定ip延迟等待30s后竞争
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException ignored) {

                }
            }
        }
        BinaryLogConfig config = getConfigByNamespace(namespace);
        applicationContext.getBean(config.getDataSourceType(),DistributorService.class).submitBinLogDistributeTask(config);
    }

    /**
     * 真正关闭数据源的逻辑
     *
     * @param namespace
     * @return
     */
    public void handleStopDatasource(String namespace) {

        if(StringUtils.isEmpty(namespace)) {
            return;
        }
        BinaryLogConfig config = getConfigByNamespace(namespace);
        applicationContext.getBean(config.getDataSourceType(),DistributorService.class).stopBinLogDistributeTask(namespace);
        logger.info("[" + namespace + "] 关闭datasource监听成功");
    }

    public void modifyBinLogConfig(BinaryLogConfig newConfig) {

        if(Thread.currentThread().isInterrupted()) {
            return;
        }

        AtomicBoolean modifyFlag = new AtomicBoolean(false);
        List<BinaryLogConfig> configList = getConfigs();
        configList = configList.stream().map((c) -> {
            if(newConfig.getNamespace().equalsIgnoreCase(c.getNamespace())) {

                // 版本号小于集群中版本号则忽略
                if(newConfig.getVersion() < c.getVersion()) {
                    logger.warn("Ignore BinLogConfig[{}] Modify case local version [{}] < current version [{}]", newConfig.getNamespace(), newConfig.getVersion(), c.getVersion());
                    return c;
                } else {
                    modifyFlag.set(true);

                    return newConfig;
                }
            }
            return c;
        }).collect(Collectors.toList());

        if(modifyFlag.get()) {
            persistConfig(configList);
        }
    }

    public List<String> getNamespaceList() {
        return getConfigs().stream()
                .map(BinaryLogConfig::getNamespace)
                .collect(Collectors.toList());
    }


    private void persistConfig(List<BinaryLogConfig> binaryLogConfigs) {

        KV kvClient = etcdClient.getKVClient();
        try {
            kvClient.put(
                    ByteSequence.from(etcdKeyPrefixUtil.withPrefix(Constants.DEFAULT_BINLOG_CONFIG_KEY), StandardCharsets.UTF_8),
                    ByteSequence.from(JSON.toJSONString(binaryLogConfigs), StandardCharsets.UTF_8)
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new EtcdServerException("Failed to connect to etcd server.", e);
        }
    }

    public static String getLocalIp(String dataSourceType){
        return dataSourceType + ":" + NetUtils.getLocalAddress().getHostAddress();
    }

}
