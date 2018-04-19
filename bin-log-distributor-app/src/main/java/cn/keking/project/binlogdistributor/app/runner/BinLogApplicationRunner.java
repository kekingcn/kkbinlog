package cn.keking.project.binlogdistributor.app.runner;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.service.BinLogEventHandler;
import cn.keking.project.binlogdistributor.app.service.BinLogEventHandlerFactory;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/11:34 AM
 * @modified by
 */
@Component
public class BinLogApplicationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BinLogApplicationRunner.class);

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    BinaryLogConfig binaryLogConfig;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    BinLogEventHandlerFactory binLogEventHandlerFactory;

    @Override
    public void run(ApplicationArguments applicationArguments) {
        // 初始化用户关注列表
        initClients();
        // 在线程中启动事件监听
        executorService.submit(() -> {
            final BinaryLogClient client = new BinaryLogClient(binaryLogConfig.getHost(), binaryLogConfig.getPort(), binaryLogConfig.getUsername(), binaryLogConfig.getPassword());
            client.registerEventListener(event -> {
                EventHeader header = event.getHeader();
                BinLogEventHandler handler = binLogEventHandlerFactory.getHandler(header);
                handler.handle(event);
            });
            client.registerLifecycleListener(new BinaryLogClient.LifecycleListener() {

                @Override
                public void onConnect(BinaryLogClient client) {
                    log.info("connect success");
                }

                @Override
                public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
                    log.error("communication fail", ex);
                }

                @Override
                public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
                    log.error("event deserialization fail", ex);
                }

                @Override
                public void onDisconnect(BinaryLogClient client) {
                    log.warn("disconnect");
                }
            });
            // 设置server id
            client.setServerId(binaryLogConfig.getServerId());
            // 配置当前位置
            configBinaryLogStatus(client);
            // 启动连接
            try {
                client.connect();
            } catch (Exception e) {
                // TODO: 17/01/2018 继续优化异常处理逻辑
                log.error("处理事件异常，{}", e);
            }
        });
    }

    /**
     * 初始化列表
     */
    private void initClients(){
        RSet<ClientInfo> clientSet = redissonClient.getSet(binaryLogConfig.getBinLogClientSet());
        clientSet.stream().forEach(c -> binLogEventHandlerFactory.addClientLocal(c));
    }

    /**
     * 配置当前binlog位置
     * @param client
     */
    private void configBinaryLogStatus(BinaryLogClient client) {
        RMap<String, Object> binLogStatus = redissonClient.getMap(binaryLogConfig.getBinLogStatusKey());
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

}
