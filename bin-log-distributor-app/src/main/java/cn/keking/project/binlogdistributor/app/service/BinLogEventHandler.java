package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.app.service.impl.BinLogWriteEventHandler;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.github.shyiko.mysql.binlog.event.*;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/3:35 PM
 * @modified by
 */
public abstract class BinLogEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BinLogWriteEventHandler.class);

    protected final static Map<Long, ColumnsTableMapEventData> TABLE_MAP_ID = new ConcurrentHashMap<>();
    protected final Map<String, Set<ClientInfo>> clientInfoMap = new ConcurrentHashMap<>();

    @Autowired
    protected BinaryLogConfig binaryLogConfig;

    @Autowired
    protected RedissonClient redissonClient;

    @Autowired
    protected DataPublisher dataPublisher;

    /**
     * 处理event
     *
     * @param event
     */
    public void handle(Event event) {
        Set<ClientInfo> clientInfos = filter(event);
        if (!CollectionUtils.isEmpty(clientInfos)) {
            publish(formatData(event), clientInfos);
            updateBinaryLogStatus(event.getHeader());
        }
    }

    /**
     * 格式化参数格式
     *
     * @param event
     * @return 格式化后的string
     */
    protected EventBaseDTO formatData(Event event) {
        return null;
    }

    /**
     * 发布信息
     *
     * @param data
     */
    protected void publish(EventBaseDTO data, Set<ClientInfo> clientInfos) {
        if (data != null) {
            log.info("推送信息,{}", data);
            dataPublisher.publish(clientInfos, data);
        }
    }

    /**
     * 添加客户端关注的表
     *
     * @param client
     */
    public void addClient(ClientInfo client) {
        String key = client.getDatabaseName().concat("/").concat(client.getTableName());
        Set<ClientInfo> clientInfos = clientInfoMap.get(key);
        if (clientInfos == null) {
            clientInfos = new HashSet<>();
            clientInfos.add(client);
            clientInfoMap.put(key, clientInfos);
        } else {
            clientInfos.add(client);
        }
    }
    /**
     * 删除客户端关注的表
     *
     * @param clientInfo
     */
    public  void  deleteClient(ClientInfo clientInfo){
        String key = clientInfo.getDatabaseName().concat("/").concat(clientInfo.getTableName());
        Set<ClientInfo> clientInfos = clientInfoMap.get(key);
        clientInfos.remove(clientInfo);
        clientInfoMap.put(key, clientInfos);
    }

    /**
     * 更新日志位置
     *
     * @param header
     */
    protected void updateBinaryLogStatus(EventHeaderV4 header) {
        RMap<String, Object> binLogStatus = redissonClient.getMap(binaryLogConfig.getBinLogStatusKey());
        binLogStatus.put("binlogPosition", header.getNextPosition());
    }


    /**
     * 筛选出关注某事件的应用列表
     * @param event
     * @return
     */
    protected Set<ClientInfo> filter(Event event) {
        return null;
    }
}
