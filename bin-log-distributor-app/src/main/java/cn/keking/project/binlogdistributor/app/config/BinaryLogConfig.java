package cn.keking.project.binlogdistributor.app.config;

/**
 * @author T-lih
 */
public class BinaryLogConfig {

    private String namespace;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private Integer serverId;

    private String dataSourceUrl;

    private String driverClassName = "com.mysql.jdbc.Driver";

    private boolean deletable = false;

    private volatile boolean active = false;

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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDataSourceUrl() {
        return dataSourceUrl;
    }

    public void setDataSourceUrl(String dataSourceUrl) {
        this.dataSourceUrl = dataSourceUrl;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
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
}
