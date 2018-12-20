package cn.keking.project.binlogdistributor.example.rabbitmq.listener;

import cn.keking.project.binlogdistributor.client.BinLogDistributorClient;
import cn.keking.project.binlogdistributor.client.rabbitmq.impl.DataSubscriberRabbitMQImpl;
import cn.keking.project.binlogdistributor.client.service.sub.DataSubscriber;
import cn.keking.project.binlogdistributor.example.rabbitmq.handler.ExampleDataEventHadler;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import com.rabbitmq.http.client.Client;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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
    private ConnectionFactory connectionFactory;

    @Autowired
    private Client rabbitHttpClient;

    @Autowired
    private RedissonClient redissonClient;

    @Value("${databaseEventServerUrl}")
    private String serverUrl;

    @Value("${appName}")
    private String appName;

    @Autowired
    private ExampleDataEventHadler exampleDatabaseEventHandler;

    @PostConstruct
    public void start() throws Exception {
        //初始化订阅的实现
        DataSubscriber dataSubscriber = new DataSubscriberRabbitMQImpl(connectionFactory, rabbitHttpClient, redissonClient);
        new BinLogDistributorClient(appName, dataSubscriber)
                //在binlog中注册handler
                .registerHandler(exampleDatabaseEventHandler)
                .setQueueType(ClientInfo.QUEUE_TYPE_RABBIT)
                .setServerUrl(serverUrl).autoRegisterClient().start();
    }
}
