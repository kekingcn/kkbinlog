package cn.keking.project.binlogdistributor.pub.impl;

import cn.keking.project.binlogdistributor.param.model.dto.*;
import org.redisson.api.RQueue;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/4:27 PM
 * @modified by chenjh
 */
public class DataPublisherRedisImpl {

    private static final Logger log = LoggerFactory.getLogger(DataPublisherRedisImpl.class);

    public static final String NOTIFIER = "BIN-LOG-NOTIFIER-";

    RedissonClient redissonClient;

    public DataPublisherRedisImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void doPublish(String clientId, String dataKey, EventBaseDTO data) {
        RQueue<EventBaseDTO> dataList = redissonClient.getQueue(dataKey);
        boolean result = dataList.offer(data);
        String notifier = NOTIFIER.concat(clientId);
        RTopic<String> rTopic = redissonClient.getTopic(notifier);
        rTopic.publish(dataKey);
    }
}
