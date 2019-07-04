package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import org.redisson.api.RedissonClient;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wanglaomo
 * @since 2019/6/4
 **/
public class BinLogEventContext {

    private DataSource dataSource;

    private RedissonClient redissonClient;

    private BinaryLogConfig binaryLogConfig;

    private DataPublisher dataPublisher;

    protected final Map<Long, ColumnsTableMapEventData> TABLE_MAP_ID = new ConcurrentHashMap<>();

    public BinLogEventContext(DataSource dataSource, RedissonClient redissonClient, BinaryLogConfig binaryLogConfig, DataPublisher dataPublisher) {
        this.dataSource = dataSource;
        this.redissonClient = redissonClient;
        this.binaryLogConfig = binaryLogConfig;
        this.dataPublisher = dataPublisher;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    public BinaryLogConfig getBinaryLogConfig() {
        return binaryLogConfig;
    }

    public DataPublisher getDataPublisher() {
        return dataPublisher;
    }

    public void addTableMapData(long tableId, ColumnsTableMapEventData data) {
        TABLE_MAP_ID.put(tableId, data);
    }

    public ColumnsTableMapEventData getTableMapData(long tableId) {
        return TABLE_MAP_ID.get(tableId);
    }
}
