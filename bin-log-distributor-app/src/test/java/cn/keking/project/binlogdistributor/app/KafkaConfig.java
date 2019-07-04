//package cn.keking.project.binlogdistributor.app;
//
//import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
//import com.google.common.collect.Maps;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.core.*;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//
//import java.util.Map;
//
///**
// * @author: kl @kailing.pub
// * @date: 2019/5/20
// */
//@Configuration
//public class KafkaConfig {
//
//    @Bean
//    public ConsumerFactory<Object, Object> consumerFactory(KafkaProperties properties) {
//        Map config = Maps.newHashMap();
//        config.put(ConsumerConfig.GROUP_ID_CONFIG, "test.kl.binlog");
//        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
//        config.putAll(properties.buildConsumerProperties());
//        return new DefaultKafkaConsumerFactory(config, null, new JsonDeserializer(EventBaseDTO.class));
//    }
//
//}
