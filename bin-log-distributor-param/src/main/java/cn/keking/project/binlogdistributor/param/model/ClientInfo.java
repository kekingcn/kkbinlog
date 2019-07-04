package cn.keking.project.binlogdistributor.param.model;

import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;
import cn.keking.project.binlogdistributor.param.enums.LockLevel;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/4:02 PM
 * @modified by
 */
public class ClientInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String QUEUE_TYPE_REDIS = "redis";
    public static final String QUEUE_TYPE_RABBIT = "rabbit";
    public static final String QUEUE_TYPE_KAFKA = "kafka";

    /**
     * 客户端编号
     */
    private String clientId;

    /**
     * 队列实现方式 默认为redis
     */
    private String queueType = QUEUE_TYPE_REDIS;

    /**
     * 关注的数据库的标识
     */
    private String namespace = "default";

    /**
     * 关注的数据库名
     */
    private String databaseName;

    /**
     * 关注的表名
     */
    private String tableName;

    /**
     * 关注的表的事件
     */
    private DatabaseEvent databaseEvent;

    /**
     * 数据锁定级别
     */
    private LockLevel lockLevel;

    /**
     * 锁级别为列的时候，使用指定列名
     */
    private String columnName;

    /**
     * 拼接key，避免频繁拼接
     */
    private String key;

    public ClientInfo() {
    }

    public ClientInfo(String clientId, String queueType, String namespace, String databaseName, String tableName, DatabaseEvent databaseEvent, LockLevel lockLevel, String columnName) {
        this.clientId = clientId;
        this.queueType = queueType;
        this.namespace = namespace == null ? "default" : namespace;
        this.databaseName = databaseName;
        this.tableName = tableName;
        this.databaseEvent = databaseEvent;
        this.lockLevel = lockLevel;
        this.columnName = columnName;

        String multiDataSourceSupport = "default".equals(this.namespace) ? "" : ("-" + namespace);

        switch (lockLevel) {
            case TABLE:
                key = clientId + multiDataSourceSupport + "-" + lockLevel + "-" + databaseName + "-" + tableName;
                break;
            case COLUMN:
                key = clientId + multiDataSourceSupport + "-" + lockLevel + "-" + databaseName + "-" + tableName + "-";
                break;
            case NONE:
            default:
                key = clientId + multiDataSourceSupport + "-" + lockLevel + "-" + databaseName;
        }
    }

    public LockLevel getLockLevel() {
        return lockLevel;
    }

    public void setLockLevel(LockLevel lockLevel) {
        this.lockLevel = lockLevel;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getQueueType() {
        return queueType;
    }

    public void setQueueType(String queueType) {
        this.queueType = queueType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public DatabaseEvent getDatabaseEvent() {
        return databaseEvent;
    }

    public void setDatabaseEvent(DatabaseEvent databaseEvent) {
        this.databaseEvent = databaseEvent;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientInfo that = (ClientInfo) o;
        return Objects.equals(clientId, that.clientId) &&
                Objects.equals(queueType, that.queueType) &&
                Objects.equals(namespace, that.namespace) &&
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(tableName, that.tableName) &&
                databaseEvent == that.databaseEvent &&
                lockLevel == that.lockLevel &&
                Objects.equals(columnName, that.columnName) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {

        return Objects.hash(clientId, queueType, databaseName, tableName, databaseEvent, lockLevel, columnName, namespace);
    }

    @Override
    public String toString() {
        return "{" +
                "\"clientId\":\"" + clientId + '\"' +
                ", \"queueType\":\"" + queueType + '\"' +
                ", \"namespace\":\"" + namespace + '\"' +
                ", \"databaseName\":\"" + databaseName + '\"' +
                ", \"tableName\":\"" + tableName + '\"' +
                ", \"databaseEvent\":\"" + databaseEvent + '\"' +
                ", \"lockLevel\":\"" + lockLevel + '\"' +
                ", \"columnName\":\"" + columnName + '\"' +
                ", \"key\":\"" + key +
                "\"}";
    }
}
