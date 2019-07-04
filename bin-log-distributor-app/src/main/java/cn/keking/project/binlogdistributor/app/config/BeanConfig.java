package cn.keking.project.binlogdistributor.app.config;

import cn.keking.project.binlogdistributor.app.service.KafkaService;
import cn.keking.project.binlogdistributor.app.service.RabbitMQService;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import cn.keking.project.binlogdistributor.pub.impl.*;
import com.rabbitmq.http.client.domain.QueueInfo;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author T-lih
 */
@Configuration
public class BeanConfig {

    private static final Logger log = LoggerFactory.getLogger(BeanConfig.class);

    @Autowired
    private RedissonClient redissonClient;

    @Bean
    DataPublisher dataPublisher(){
        return new DataPublisher();
    }

    @Bean
    public DataPublisherRedisImpl dataPublisherRedisImpl() {
        DataPublisherRedisImpl dataPublisher = null;
        try {
            dataPublisher = new DataPublisherRedisImpl(redissonClient);
        } catch (Exception e) {
            log.error("初始化redis队列实现失败", e);
        }
        return dataPublisher;
    }

    @Bean
    @ConditionalOnProperty("spring.rabbit.host")
    public DataPublisherRabbitMQ dataPublisherRabbitMQ() {
        DataPublisherRabbitMQImpl dataPublisher = null;
        try {
            dataPublisher = new DataPublisherRabbitMQImpl();
        } catch (Exception e) {
            log.error("初始化rabbit队列实现失败", e);
        }
        return dataPublisher;
    }

    @Bean
    @ConditionalOnMissingBean(DataPublisherRabbitMQ.class)
    public DataPublisherRabbitMQ noOpDataPublisherRabbitMQ() {
        log.info("Can't find RabbitMQ config，using muted Publisher");
        return new NoOpDataPublisherRabbitMQ();
    }

    @Bean
    @ConditionalOnMissingBean(RabbitMQService.class)
    public RabbitMQService rabbitMQService() {
        return new RabbitMQService() {
            @Override
            public QueueInfo getQueue(String clientId) {return null;}

            @Override
            public List<EventBaseDTO> getMessageList(String clientId, long count) {return new ArrayList<>();}
        };
    }


    @Bean
    @ConditionalOnProperty("spring.kafka.bootstrap-servers")
    public DataPublisherKafka dataPublisherKafka(KafkaTemplate<String,Object> kafkaTemplate) {
        DataPublisherKafkaImpl dataPublisher = null;
        try {
            dataPublisher = new DataPublisherKafkaImpl(kafkaTemplate);
        } catch (Exception e) {
            log.error("初始化队列实现失败", e);
        }
        return dataPublisher;
    }

    @Bean
    @ConditionalOnMissingBean(DataPublisherKafka.class)
    public DataPublisherKafka noOpDataPublisherKafka() {
        log.info("Can't find Kafka config，using muted Publisher");
        return new NoOpDataPublisherKafka();
    }

    @Bean
    @ConditionalOnMissingBean(KafkaService.class)
    public KafkaService kafkaService() {
        return new KafkaService() {
            @Override
            public void createTopic(String topicName, int partitions, int replication) {}

            @Override
            public void createKafkaTopic(ClientInfo clientInfo) {}
        };
    }

    /**
     * @return 下面两个配置为跨域访问的配置
     */
    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildConfig());
        return new CorsFilter(source);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            Environment env = ctx.getEnvironment();
            log.info("server.port=>{}",env.getProperty("server.port"));
            log.info("spring.redisson.address=>{}",env.getProperty("spring.redisson.address"));
            log.info("spring.redisson.database=>{}", env.getProperty("spring.redisson.database"));
            String[] beanDefinitionNames =  ctx.getBeanDefinitionNames();
            Arrays.stream(beanDefinitionNames).sorted().forEach(val ->{
            });
        };
    }


}
