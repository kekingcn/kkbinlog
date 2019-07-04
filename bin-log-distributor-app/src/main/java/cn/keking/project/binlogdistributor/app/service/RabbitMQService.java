package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import com.rabbitmq.http.client.domain.QueueInfo;

import java.util.List;

/**
 * @author wanglaomo
 * @since 2019/6/6
 **/
public interface RabbitMQService {

    QueueInfo getQueue(String clientId);

    List<EventBaseDTO> getMessageList(String clientId, long count);
}
