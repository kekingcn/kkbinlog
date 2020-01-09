package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.service.EtcdService;
import cn.keking.project.binlogdistributor.app.util.EtcdKeyPrefixUtil;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.zaxxer.hikari.HikariDataSource;
import io.etcd.jetcd.*;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author wanglaomo
 * @since 2019/6/4
 **/
@Component
public class BinLogClientFactory {

    private static final Logger log = LoggerFactory.getLogger(BinLogClientFactory.class);

    protected final RedissonClient redissonClient;

    private final DataPublisher dataPublisher;

    private final EtcdService etcdService;

    private final Client etcdClient;

    private final BinaryLogConfigContainer binaryLogConfigContainer;

    private final EtcdKeyPrefixUtil etcdKeyPrefixUtil;

    private volatile Set<String> activeNameSpaces = ConcurrentHashMap.newKeySet();

    private volatile AtomicLong eventCount = new AtomicLong(0);

    private long lastEventCount = 0;

    public BinLogClientFactory(RedissonClient redissonClient, @Qualifier("binlogDataPublisher") DataPublisher dataPublisher, EtcdService etcdService, Client etcdClient, BinaryLogConfigContainer binaryLogConfigContainer, EtcdKeyPrefixUtil etcdKeyPrefixUtil) {
        this.redissonClient = redissonClient;
        this.dataPublisher = dataPublisher;
        this.etcdService = etcdService;
        this.etcdClient = etcdClient;
        this.binaryLogConfigContainer = binaryLogConfigContainer;
        this.etcdKeyPrefixUtil = etcdKeyPrefixUtil;
    }

    public BinaryLogClient initClient(BinaryLogConfig binaryLogConfig) {

        String namespace = binaryLogConfig.getNamespace();

        DataSource dataSource = getDataSource(binaryLogConfig);

        BinLogEventContext context = new BinLogEventContext(dataSource, binaryLogConfig, etcdService, dataPublisher);
        BinLogEventHandlerFactory binLogEventHandlerFactory = new BinLogEventHandlerFactory(context);

        // 初始化用户关注列表
        initClients(binaryLogConfig, binLogEventHandlerFactory);

        final BinaryLogClient client = new BinaryLogClient(binaryLogConfig.getHost(), binaryLogConfig.getPort(), binaryLogConfig.getUsername(), binaryLogConfig.getPassword());
        client.registerEventListener(event -> {
            eventCount.incrementAndGet();
            EventHeader header = event.getHeader();
            BinLogEventHandler handler = binLogEventHandlerFactory.getHandler(header);
            handler.handle(event);
        });
        client.registerLifecycleListener(new BinaryLogClient.LifecycleListener() {

            @Override
            public void onConnect(BinaryLogClient client) {
                binaryLogConfig.setActive(true);
                binaryLogConfig.setVersion(binaryLogConfig.getVersion() + 1);
                binaryLogConfigContainer.modifyBinLogConfig(binaryLogConfig);
                log.info("[" + namespace + "] connect success");
            }
            @Override
            public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
                log.error("[" + namespace + "] communication fail", ex);
                binaryLogConfig.setActive(false);
                binaryLogConfigContainer.modifyBinLogConfig(binaryLogConfig);
            }
            @Override
            public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
                log.error("[" + namespace + "] event deserialization fail", ex);
                binaryLogConfig.setActive(false);
                binaryLogConfigContainer.modifyBinLogConfig(binaryLogConfig);
            }
            @Override
            public void onDisconnect(BinaryLogClient client) {
                log.warn("[" + namespace + "] disconnect");
                binaryLogConfig.setActive(false);
                binaryLogConfigContainer.modifyBinLogConfig(binaryLogConfig);
            }
        });
        // 设置server id
        client.setServerId(binaryLogConfig.getServerId());
        // 配置当前位置
        configBinaryLogStatus(client, binaryLogConfig);

        // 启动Client列表数据监听
        registerMetaDataWatcher(binaryLogConfig, binLogEventHandlerFactory);

        // 记录active namespaces
        activeNameSpaces.add(namespace);

        return client;
    }

    /**
     * 初始化列表
     * @param binaryLogConfig
     * @param binLogEventHandlerFactory
     */
    private void initClients(BinaryLogConfig binaryLogConfig, BinLogEventHandlerFactory binLogEventHandlerFactory){

        List<ClientInfo> clientSet = etcdService.listBinLogConsumerClient(binaryLogConfig);
        clientSet.forEach(binLogEventHandlerFactory::addClientLocal);
    }

    /**
     * 配置当前binlog位置
     * @param client
     * @param binaryLogConfig
     */
    private void configBinaryLogStatus(BinaryLogClient client, BinaryLogConfig binaryLogConfig) {

        JSONObject binLogStatus = etcdService.getBinaryLogStatus(binaryLogConfig);

        if (binLogStatus != null) {
            Object binlogFilename = binLogStatus.get("binlogFilename");
            if (binlogFilename != null) {
                client.setBinlogFilename((String) binlogFilename);
            }
            Long binlogPosition = binLogStatus.getLong("binlogPosition");
            if (binlogPosition != null) {
                client.setBinlogPosition(binlogPosition);
            }
        }
    }

    private DataSource getDataSource(BinaryLogConfig binaryLogConfig) {
        return DataSourceBuilder
                .create(BinLogClientFactory.class.getClassLoader())
                .type(HikariDataSource.class)
                .driverClassName(binaryLogConfig.getDriverClassName())
                .url(binaryLogConfig.getDataSourceUrl())
                .username(binaryLogConfig.getUsername())
                .password(binaryLogConfig.getPassword()).build();
    }

    /**
     * 注册Client列表更新监听
     * @param binaryLogConfig
     * @param binLogEventHandlerFactory
     */
    private void registerMetaDataWatcher(BinaryLogConfig binaryLogConfig, BinLogEventHandlerFactory binLogEventHandlerFactory) {

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
                        for(WatchEvent event: eventList) {
                            if(WatchEvent.EventType.PUT.equals(event.getEventType())) {

                                KeyValue currentKV = event.getKeyValue();
                                Set<ClientInfo> currentClientInfoSet = getClientInfos(currentKV);

                                currentClientInfoSet
                                        .stream()
                                        .collect(Collectors.groupingBy(ClientInfo::getDatabaseEvent))
                                        .forEach(binLogEventHandlerFactory::updateClientBatch);
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

    public boolean closeClient(BinaryLogClient binaryLogClient, String namespace) {

        try {
            // remove active namespace
            activeNameSpaces.remove(namespace);

            binaryLogClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        BinaryLogConfig binaryLogConfig = binaryLogConfigContainer.getConfigByNamespace(namespace);
        try {
            while(binaryLogConfig.isActive()) {
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
        long res =  total - lastEventCount;

        lastEventCount = total;

        return res;
    }
}
