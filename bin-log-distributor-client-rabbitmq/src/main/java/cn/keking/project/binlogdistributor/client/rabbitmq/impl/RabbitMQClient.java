package cn.keking.project.binlogdistributor.client.rabbitmq.impl;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @auther: chenjh
 * @time: 2018/11/20 10:34
 * @description
 */
public class RabbitMQClient {

    private static final String NOTIFY_EXCHANGE = "binlog.notify";
    private static final String DATA_EXCHANGE = "binlog.data";

    private static RabbitMQClient instance;

    private ConnectionFactory connectionFactory;

    private RabbitMQClient() {}

    public synchronized static RabbitMQClient getInstance(ConnectionFactory connectionFactory) throws Exception {
        if (instance == null && connectionFactory != null) {
            instance = new RabbitMQClient();
            instance.connectionFactory = connectionFactory;
            instance.amqpAdmin = new RabbitAdmin(connectionFactory);
            instance.amqpTemplate = new RabbitTemplate(connectionFactory);
            instance.setDataExchange();
            instance.setNotifyExchange();
        }
        return instance;
    }

    private AmqpAdmin amqpAdmin;

    private AmqpTemplate amqpTemplate;

    private DirectExchange notifyExchange;

    private TopicExchange dataExchange;

    public AmqpAdmin getAmqpAdmin() {
        return amqpAdmin;
    }

    public AmqpTemplate getAmqpTemplate() {
        return amqpTemplate;
    }

    public DirectExchange getNotifyExchange() {
        return notifyExchange;
    }

    public TopicExchange getDataExchange() {
        return dataExchange;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    private void setNotifyExchange() {
        DirectExchange notifyExchange = new DirectExchange(NOTIFY_EXCHANGE,true,false);
        amqpAdmin.declareExchange(notifyExchange);
        this.notifyExchange =  notifyExchange;
    }

    private void setDataExchange() {
        TopicExchange dataExchange = new TopicExchange(DATA_EXCHANGE,true,false);
        amqpAdmin.declareExchange(dataExchange);
        this.dataExchange =  dataExchange;
    }
}
