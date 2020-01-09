package cn.keking.project.binlogdistributor.pub.impl;

import kafka.admin.AdminUtils;
import org.I0Itec.zkclient.ZkClient;
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

    private ZkClient zkClient;

    private KafkaTemplate<String, Object> kafkaTemplate;

    public DataPublisherKafkaImpl(KafkaTemplate kafkaTemplate, ZkClient zkClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.zkClient = zkClient;
    }

    @Override
    public void doPublish(String topic, Object data) {
        ListenableFuture<SendResult<String, Object>> reuslt = kafkaTemplate.send(topic, data);
        reuslt.addCallback(success -> log.info("推送消息到Kafka:{}", data)
        , failure -> log.error("推送消息到Kafka失败:" + data.toString(), failure.getCause()));
    }

    @Override
    public boolean deleteTopic(String topicName) {

        try {
            AdminUtils.deleteTopic(zkClient, topicName);
        } catch (Exception e) {
            log.error("kafak删除topic失败", e);
            return false;
        }
        return true;
    }
}
