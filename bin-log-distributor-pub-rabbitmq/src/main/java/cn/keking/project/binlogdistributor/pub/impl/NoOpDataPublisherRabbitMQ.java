package cn.keking.project.binlogdistributor.pub.impl;

import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wanglaomo
 * @since 2019/6/5
 **/
public class NoOpDataPublisherRabbitMQ implements DataPublisherRabbitMQ {

    private static final Logger logger = LoggerFactory.getLogger(NoOpDataPublisherRabbitMQ.class);

    @Override
    public void doPublish(String clientId, String dataKey, EventBaseDTO data) {
        logger.warn("System don't support rabbitmq data publisher, however it has been subscribed by client:[{}]", clientId);
    }
}
