package cn.keking.project.binlogdistributor.app.config;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.Util;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author wanglaomo
 * @since 2019/8/2
 **/
@Configuration
@ConfigurationProperties("spring.etcd")
public class EtcdConfig {

    private List<String> endpoints;

    private String authority;

    private String username;

    private String password;

    private String root;

    @Bean
    public Client etcdClient() {

        Client client  = Client
                .builder()
                .endpoints(Util.toURIs(endpoints))
                //.authority(authority)
                //.user(ByteSequence.from(username, StandardCharsets.UTF_8))
                //.password(ByteSequence.from(password, StandardCharsets.UTF_8))
                .build();

        return client;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}
