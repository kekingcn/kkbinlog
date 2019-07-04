package cn.keking.project.binlogdistributor.pub.impl;

import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;

public interface DataPublisherRabbitMQ {
    void doPublish(String clientId, String dataKey, EventBaseDTO data);
}
