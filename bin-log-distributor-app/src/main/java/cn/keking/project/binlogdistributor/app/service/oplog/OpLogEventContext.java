package cn.keking.project.binlogdistributor.app.service.oplog;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.app.service.EtcdService;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.mongodb.MongoClient;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: kl @kailing.pub
 * @date: 2020/1/7
 */
public class OpLogEventContext {

    private MongoClient  mongoClient;

    private EtcdService etcdService;

    private BinaryLogConfig binaryLogConfig;

    private DataPublisher dataPublisher;

    protected final Map<Long, ColumnsTableMapEventData> TABLE_MAP_ID = new ConcurrentHashMap<>();

    public OpLogEventContext(MongoClient mongoClient, EtcdService etcdService, BinaryLogConfig binaryLogConfig, DataPublisher dataPublisher) {
        this.mongoClient = mongoClient;
        this.etcdService = etcdService;
        this.binaryLogConfig = binaryLogConfig;
        this.dataPublisher = dataPublisher;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public void setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
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

    public EtcdService getEtcdService() {
        return etcdService;
    }
}
