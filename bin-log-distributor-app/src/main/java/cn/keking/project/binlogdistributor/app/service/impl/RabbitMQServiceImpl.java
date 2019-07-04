package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.service.RabbitMQService;
import cn.keking.project.binlogdistributor.app.util.RabbitMQHttpClient;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import com.rabbitmq.http.client.domain.QueueInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wanglaomo
 * @since 2019/6/5
 **/
@Service
@ConditionalOnProperty("spring.rabbit.host")
public class RabbitMQServiceImpl implements RabbitMQService {

    @Value("${spring.rabbit.virtualHost}")
    private String vHost;

    @Autowired
    RabbitMQHttpClient rabbitMQHttpClient;

    @Override
    public QueueInfo getQueue(String clientId) {

        return rabbitMQHttpClient.getQueue(vHost, clientId);
    }

    @Override
    public List<EventBaseDTO> getMessageList(String clientId, long count) {

        return rabbitMQHttpClient.getMessageList(vHost, clientId, count);
    }
}
