package cn.keking.project.binlogdistributor.app.config;

import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: kl @kailing.pub
 * @date: 2019/5/24
 */
@Configuration
@ConfigurationProperties(prefix = "kafka.zk")
@ConditionalOnProperty("spring.kafka.bootstrap-servers")
public class KafkaConfig {

    private String servers = "192.168.1.81:2181";
    private int connectionTimeout = 30000;
    private int sessionTimeout = 30000;
    /**
     * kafka默认分区数
     */
    private int partitions = 1;
    /**
     * kafka默认副本数
     */
    private int replications = 1;

    @Bean
    public ZkClient zkClient() {
        return new ZkClient(servers, sessionTimeout, connectionTimeout, ZKStringSerializer$.MODULE$);
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public int getReplications() {
        return replications;
    }

    public void setReplications(int replications) {
        this.replications = replications;
    }
}
