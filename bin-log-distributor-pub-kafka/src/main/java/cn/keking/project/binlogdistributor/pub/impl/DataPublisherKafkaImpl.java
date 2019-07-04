package cn.keking.project.binlogdistributor.pub.impl;

import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @author: kl @kailing.pub
 * @date: 2019/5/20
 */
public class DataPublisherKafkaImpl implements DataPublisherKafka {

    private static final Logger log = LoggerFactory.getLogger(DataPublisherKafkaImpl.class);

    private KafkaTemplate<String, Object> kafkaTemplate;

    public DataPublisherKafkaImpl(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void doPublish(String clientId, String dataKey, EventBaseDTO data) {
        ListenableFuture<SendResult<String, Object>> reuslt = kafkaTemplate.send(dataKey, data);
        reuslt.addCallback(success -> log.info("推送消息到Kafka:{}", data)
        , failure -> log.error("推送消息到Kafka失败:" + data.toString(), failure.getCause()));
    }
}
