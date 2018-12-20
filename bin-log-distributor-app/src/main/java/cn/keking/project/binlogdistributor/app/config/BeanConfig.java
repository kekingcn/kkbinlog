package cn.keking.project.binlogdistributor.app.config;

import cn.keking.project.binlogdistributor.pub.DataPublisher;
import cn.keking.project.binlogdistributor.pub.impl.DataPublisherRabbitMQImpl;
import cn.keking.project.binlogdistributor.pub.impl.DataPublisherRedisImpl;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/6:17 PM
 * @modified by chenjh
 */
@Configuration
public class BeanConfig {

    private static final Logger log = LoggerFactory.getLogger(BeanConfig.class);


    @Autowired
    RedissonClient redissonClient;

    @Bean
    DataPublisher dataPublisher(){
        return new DataPublisher();
    }

    @Bean
    DataPublisherRedisImpl dataPublisherRedisImpl() {
        DataPublisherRedisImpl dataPublisher = null;
        try {
            dataPublisher = new DataPublisherRedisImpl(redissonClient);
        } catch (Exception e) {
            log.error("初始化redis队列实现失败", e);
        }
        return dataPublisher;
    }

    @Bean
    DataPublisherRabbitMQImpl dataPublisherRabbitMQ() {
        DataPublisherRabbitMQImpl dataPublisher = null;
        try {
            dataPublisher = new DataPublisherRabbitMQImpl();
        } catch (Exception e) {
            log.error("初始化rabbit队列实现失败", e);
        }
        return dataPublisher;
    }

}
