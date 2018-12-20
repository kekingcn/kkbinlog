package cn.keking.project.binlogdistributor.client.rabbitmq.impl;

import cn.keking.project.binlogdistributor.client.BinLogDistributorClient;
import cn.keking.project.binlogdistributor.client.service.sub.DataSubscriber;
import com.rabbitmq.client.*;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.QueueInfo;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @auther: chenjh
 * @time: 2018/11/20 10:18
 * @description
 */
public class DataSubscriberRabbitMQImpl implements DataSubscriber {

    public static final String NOTIFIER = "BIN-LOG-NOTIFIER-";
    public static final String DATA = "BIN-LOG-DATA-";

    public Client rabbitHttpClient;

    public String vhost;

    private RabbitMQClient rabbitMQClient;

    private RedissonClient redissonClient;

    private static final ExecutorService executors = Executors.newFixedThreadPool(5);

    public DataSubscriberRabbitMQImpl (ConnectionFactory connectionFactory, Client rabbitHttpClient, RedissonClient redissonClient) throws Exception {
        this.rabbitMQClient = RabbitMQClient.getInstance(connectionFactory);
        this.rabbitHttpClient = rabbitHttpClient;
        this.vhost = this.rabbitMQClient.getConnectionFactory().getVirtualHost();
        this.redissonClient = redissonClient;
    }

    @Override
    public void subscribe(String clientId, BinLogDistributorClient binLogDistributorClient) {
        List<QueueInfo> queueList = rabbitHttpClient.getQueues(vhost);
        ConnectionFactory connectionFactory = rabbitMQClient.getConnectionFactory();
        //处理历史数据
        queueList.stream().filter(queueInfo -> queueInfo.getName().startsWith(DATA + clientId) && !queueInfo.getName().endsWith("-Lock"))
                .forEach(queueInfo -> executors.submit(new DataHandler(queueInfo.getName(), clientId, binLogDistributorClient, redissonClient, connectionFactory)));
        try {
            Channel channel = connectionFactory.createConnection().createChannel(false);
                channel.queueDeclare(NOTIFIER + clientId, true, false, true, null);
                Consumer consumer = new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String msg = new String(body);
                        //每次推送都会执行这个方法，每次开线程，使用线程里面redis锁判断开销太大，先在外面判断一次
                        if (!DataHandler.DATA_KEY_IN_PROCESS.contains(msg)) {
                            //如果没在处理再进入
                            executors.submit(new DataHandler(msg, clientId, binLogDistributorClient, redissonClient, connectionFactory));
                        }
                    }
                };
            channel.basicConsume(NOTIFIER + clientId, true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
