package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.app.service.EtcdService;
import cn.keking.project.binlogdistributor.pub.DataPublisher;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wanglaomo
 * @since 2019/6/4
 **/
public class BinLogEventContext {

    private DataSource dataSource;

    private EtcdService etcdService;

    private BinaryLogConfig binaryLogConfig;

    private DataPublisher dataPublisher;

    protected final Map<Long, ColumnsTableMapEventData> TABLE_MAP_ID = new ConcurrentHashMap<>();

    public BinLogEventContext(DataSource dataSource, BinaryLogConfig binaryLogConfig, EtcdService etcdService, DataPublisher dataPublisher) {
        this.dataSource = dataSource;
        this.binaryLogConfig = binaryLogConfig;
        this.dataPublisher = dataPublisher;
        this.etcdService = etcdService;
    }

    public DataSource getDataSource() {
        return dataSource;
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
