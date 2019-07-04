package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.zaxxer.hikari.HikariDataSource;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * @author wanglaomo
 * @since 2019/6/4
 **/
@Component
public class BinLogClientFactory {

    private static final Logger log = LoggerFactory.getLogger(BinLogClientFactory.class);

    @Autowired
    protected RedissonClient redissonClient;

    @Autowired
    private DataPublisher dataPublisher;

    private final Map<String, BinLogEventHandlerFactory> HANDLER_FACTORY_MAP = new ConcurrentHashMap<>();

    private final Map<String, BinaryLogConfig> CONFIG_MAP = new ConcurrentHashMap<>();

    private final Map<String, BinaryLogClient> CLIENT_MAP = new ConcurrentHashMap<>();

    public BinaryLogClient initClient(BinaryLogConfig binaryLogConfig, Lock lock, Condition startCondition) {

        String namespace = binaryLogConfig.getNamespace();

        DataSource dataSource = getDataSource(binaryLogConfig);

        BinLogEventContext context = new BinLogEventContext(dataSource, redissonClient, binaryLogConfig, dataPublisher);
        BinLogEventHandlerFactory binLogEventHandlerFactory = new BinLogEventHandlerFactory(context);

        // 初始化用户关注列表
        initClients(binaryLogConfig, binLogEventHandlerFactory);

        final BinaryLogClient client = new BinaryLogClient(binaryLogConfig.getHost(), binaryLogConfig.getPort(), binaryLogConfig.getUsername(), binaryLogConfig.getPassword());
        client.registerEventListener(event -> {
            EventHeader header = event.getHeader();
            BinLogEventHandler handler = binLogEventHandlerFactory.getHandler(header);
            handler.handle(event);
        });
        client.registerLifecycleListener(new BinaryLogClient.LifecycleListener() {

            @Override
            public void onConnect(BinaryLogClient client) {
                binaryLogConfig.setActive(true);
                if(lock != null) {
                    lock.lock();
                    try {
                        startCondition.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }

                log.info("[" + namespace + "] connect success");
            }
            @Override
            public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
                log.error("[" + namespace + "] communication fail", ex);
            }
            @Override
            public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
                log.error("[" + namespace + "] event deserialization fail", ex);
            }
            @Override
            public void onDisconnect(BinaryLogClient client) {
                binaryLogConfig.setActive(false);
                log.warn("[" + namespace + "] disconnect");
            }
        });
        // 设置server id
        client.setServerId(binaryLogConfig.getServerId());
        // 配置当前位置
        configBinaryLogStatus(client, binaryLogConfig);

        // 保存EventHandlerFactory
        HANDLER_FACTORY_MAP.put(namespace, binLogEventHandlerFactory);
        CONFIG_MAP.put(namespace, binaryLogConfig);
        CLIENT_MAP.put(namespace, client);

        return client;
    }

    public BinLogEventHandlerFactory getBinLogEventHandlerFactory(String namespace) {

        return HANDLER_FACTORY_MAP.get(namespace);
    }

    public BinaryLogConfig getBinaryLogConfig(String namespace) {

        return CONFIG_MAP.get(namespace);
    }

    public BinaryLogClient getBinaryLogClient(String namespace) {

        return CLIENT_MAP.get(namespace);
    }

    public List<String> getNamespaceList() {

        return new ArrayList<>(CONFIG_MAP.keySet());
    }

    public List<BinaryLogConfig> getConfigList() {

        return new ArrayList<>(CONFIG_MAP.entrySet().stream().map(e -> e.getValue()).collect(Collectors.toList()));
    }

    /**
     * 初始化列表
     * @param binaryLogConfig
     * @param binLogEventHandlerFactory
     */
    private void initClients(BinaryLogConfig binaryLogConfig, BinLogEventHandlerFactory binLogEventHandlerFactory){

        String namespace = binaryLogConfig.getNamespace();
        String binLogClientSet = binaryLogConfig.getBinLogClientSet();
        RSet<ClientInfo> clientSet = redissonClient.getSet(keyPrefix(namespace,binLogClientSet));
        clientSet.stream().forEach(c -> binLogEventHandlerFactory.addClientLocal(c));
    }

    /**
     * 配置当前binlog位置
     * @param client
     * @param binaryLogConfig
     */
    private void configBinaryLogStatus(BinaryLogClient client, BinaryLogConfig binaryLogConfig) {

        String namespace = binaryLogConfig.getNamespace();
        String binLogStatusKey = binaryLogConfig.getBinLogStatusKey();

        RMap<String, Object> binLogStatus = redissonClient.getMap(keyPrefix(namespace, binLogStatusKey));
        if (binLogStatus != null) {
            Object binlogFilename = binLogStatus.get("binlogFilename");
            if (binlogFilename != null) {
                client.setBinlogFilename((String) binlogFilename);
            }
            Object binlogPosition = binLogStatus.get("binlogPosition");
            if (binlogPosition != null) {
                client.setBinlogPosition((Long) binlogPosition);
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

    private String keyPrefix(String namespace, String key) {

        StringBuilder builder = new StringBuilder();

        return builder
                .append(Constants.REDIS_PREFIX)
                .append(namespace)
                .append("::")
                .append(key)
                .toString();
    }
}
