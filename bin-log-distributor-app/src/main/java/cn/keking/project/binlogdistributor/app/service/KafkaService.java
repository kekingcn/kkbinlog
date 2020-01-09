package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.param.model.ClientInfo;

/**
 * @author wanglaomo
 * @since 2019/6/6
 **/
public interface KafkaService {

    void createKafkaTopic(ClientInfo clientInfo, Integer partitions, Integer replication);
}
