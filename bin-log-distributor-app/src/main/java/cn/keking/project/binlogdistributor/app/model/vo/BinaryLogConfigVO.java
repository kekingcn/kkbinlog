package cn.keking.project.binlogdistributor.app.model.vo;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;

/**
 * @author wanglaomo
 * @since 2019/6/11
 **/
public class BinaryLogConfigVO {

    private String namespace;

    private String host;

    private Integer port;

    private String username;

    private Integer serverId;

    private String dataSourceUrl;

    private boolean deletable;

    private boolean active;

    private String binLogStatusKey;

    private String binLogClientSet;

    public BinaryLogConfigVO(BinaryLogConfig config) {
        this.namespace = config.getNamespace();
        this.host = config.getHost();
        this.port = config.getPort();
        this.username = config.getUsername();
        this.serverId = config.getServerId();
        this.dataSourceUrl = config.getDataSourceUrl();
        this.deletable = config.isDeletable();
        this.active = config.isActive();
        this.binLogClientSet = config.getBinLogClientSet();
        this.binLogStatusKey = config.getBinLogStatusKey();
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

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

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public String getDataSourceUrl() {
        return dataSourceUrl;
    }

    public void setDataSourceUrl(String dataSourceUrl) {
        this.dataSourceUrl = dataSourceUrl;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
