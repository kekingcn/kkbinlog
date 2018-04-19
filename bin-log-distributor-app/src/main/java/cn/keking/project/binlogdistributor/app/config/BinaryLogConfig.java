package cn.keking.project.binlogdistributor.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhenhui
 * @date Created in 2018/16/01/2018/6:22 PM
 * @modified by
 */
@ConfigurationProperties(prefix = "binaryLog")
@Configuration
public class BinaryLogConfig {

    private String host;
    private Integer port;
    private String username;
    private String password;
    private Integer serverId;
    /**
     * 在redis中保存状态的key名称
     */
    private String binLogStatusKey = "binLogStatus";
    /**
     * 在redis中保存状态的key名称
     */
    private String binLogClientSet = "binLogClientSet";

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
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

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public String getBinLogStatusKey() {
        return binLogStatusKey;
    }

    public void setBinLogStatusKey(String binLogStatusKey) {
        this.binLogStatusKey = binLogStatusKey;
    }

    public String getBinLogClientSet() {
        return binLogClientSet;
    }

    public void setBinLogClientSet(String binLogClientSet) {
        this.binLogClientSet = binLogClientSet;
    }
}
