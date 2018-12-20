package cn.keking.project.binlogdistributor.app.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @auther: chenjh
 * @time: 2018/11/19 9:01
 * @description
 */
@ConfigurationProperties(prefix = "spring.rabbit")
@Configuration
public class RabbitMQConfig {

    public static final String NOTIFY_EXCHANGE = "binlog.notify";
    public static final String DATA_EXCHANGE = "binlog.data";

    private String host;

    private String port;

    private String username;

    private String password;

    private String virtualHost;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setAddresses(host + ":"  + port);
        factory.setVirtualHost(virtualHost);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean("notifyExchange")
    public DirectExchange notifyExchange(ConnectionFactory connectionFactory) {
        DirectExchange notifyExchange = new DirectExchange(NOTIFY_EXCHANGE,true,false);
        new RabbitAdmin(connectionFactory).declareExchange(notifyExchange);
        return notifyExchange;
    }

    @Bean("dataExchange")
    public TopicExchange dataExchange(ConnectionFactory connectionFactory) {
        TopicExchange dataExchange = new TopicExchange(DATA_EXCHANGE,true,false);
        new RabbitAdmin(connectionFactory).declareExchange(dataExchange);
        return dataExchange;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }
}
