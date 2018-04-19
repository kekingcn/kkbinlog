package cn.keking.project.binlogdistributor.app.config;

import cn.keking.project.binlogdistributor.pub.DataPublisher;
import cn.keking.project.binlogdistributor.pub.impl.DataPublisherRedisImpl;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/6:17 PM
 * @modified by
 */
@Configuration
public class BeanConfig {

    @Autowired
    RedissonClient redissonClient;

    @Bean
    DataPublisher dataPublisher(){
        return new DataPublisherRedisImpl(redissonClient);
    }
}
