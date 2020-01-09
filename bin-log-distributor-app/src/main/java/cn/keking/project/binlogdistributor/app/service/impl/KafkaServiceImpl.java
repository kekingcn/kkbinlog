package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.config.KafkaConfig;
import cn.keking.project.binlogdistributor.app.service.KafkaService;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import kafka.admin.AdminUtils;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * @author: kl @kailing.pub
 * @date: 2019/5/24
 */
@Service
@ConditionalOnProperty("spring.kafka.bootstrap-servers")
public class KafkaServiceImpl implements KafkaService {

    @Autowired
    private ZkClient zkClient;

    @Autowired
    private KafkaConfig kafkaConfig;

    /**
     * 创建Topic
     * @param clientInfo
     */
    @Override
    public void createKafkaTopic(ClientInfo clientInfo, Integer partitions, Integer replication){
        String topicName = DataPublisher.topicName(clientInfo);
        if(partitions == null || replication == null) {
            createTopic(topicName);
        } else {
            createTopic(topicName, partitions, replication);
        }
    }

    private void createTopic(String topicName, Integer partitions, Integer replication) {
        Boolean isExists =AdminUtils.topicExists(zkClient,topicName);
        if(!isExists){
            AdminUtils.createTopic(zkClient,topicName,partitions,replication,new Properties());
        }
    }

    private void createTopic(String topicName){
         createTopic(topicName,kafkaConfig.getPartitions(),kafkaConfig.getReplications());
    }

}
