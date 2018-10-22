package cn.keking.project.binlogdistributor.example.listener;

import cn.keking.project.binlogdistributor.client.BinLogDistributorClient;
import cn.keking.project.binlogdistributor.client.redis.impl.DataSubscriberRedisImpl;
import cn.keking.project.binlogdistributor.client.service.sub.DataSubscriber;
import cn.keking.project.binlogdistributor.example.handler.ExampleDataEventHadler;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @auther: chenjh
 * @time: 2018/10/16 11:38
 * @description
 */
@Component
public class ExampleEventListener {

    @Autowired
    private RedissonClient redissonClient;

    @Value("${databaseEventServerUrl}")
    private String serverUrl;

    @Value("${appName}")
    private String appName;

    @Autowired
    private ExampleDataEventHadler exampleDatabaseEventHandler;

    @PostConstruct
    public void start() {
        //初始化订阅的实现
        DataSubscriber dataSubscriber = new DataSubscriberRedisImpl(redissonClient);
        new BinLogDistributorClient(appName, dataSubscriber)
                //在binlog中注册handler
                .registerHandler(exampleDatabaseEventHandler)
                .setServerUrl(serverUrl).autoRegisterClient().start();
    }
}
