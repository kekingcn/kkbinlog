package cn.keking.project.binlogdistributor.client.redis.impl;

import cn.keking.project.binlogdistributor.client.BinLogDistributorClient;
import cn.keking.project.binlogdistributor.client.service.sub.DataSubscriber;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/4:27 PM
 * @modified by
 */
public class DataSubscriberRedisImpl implements DataSubscriber {
    private final static Logger log = Logger.getLogger(DataSubscriberRedisImpl.class.toString());

    public static final String NOTIFIER = "BIN-LOG-NOTIFIER-";
    public static final String DATA = "BIN-LOG-DATA-";

    private static final ExecutorService executors = Executors.newFixedThreadPool(5);

    RedissonClient redissonClient;

    public DataSubscriberRedisImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 订阅信息
     *
     * @param clientId
     */
    @Override
    public void subscribe(String clientId, BinLogDistributorClient binLogDistributorClient) {
        Collection<String> keysByPattern = redissonClient.getKeys().findKeysByPattern(DATA+ clientId + "*");
        //处理历史的
        keysByPattern.stream().filter(k -> !k.endsWith("-Lock"))
                .forEach(k -> executors.submit(new DataHandler(k, clientId, binLogDistributorClient, redissonClient)));
        RTopic<String> topic = redissonClient.getTopic(NOTIFIER.concat(clientId));
        topic.addListener((channel, msg) -> {
            if (!DataHandler.DATA_KEY_IN_PROCESS.contains(msg)) {
                //如果没在处理再进入
                executors.submit(new DataHandler(msg, clientId, binLogDistributorClient, redissonClient));
            }
        });
    }

}
