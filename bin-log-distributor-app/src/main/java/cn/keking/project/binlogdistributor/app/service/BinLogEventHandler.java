package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.github.shyiko.mysql.binlog.event.*;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * @author T-lih
 */
public abstract class BinLogEventHandler {

    protected BinLogEventContext context;

    protected final Map<String, Set<ClientInfo>> clientInfoMap = new ConcurrentHashMap<>();

    public BinLogEventHandler(BinLogEventContext context) {
        this.context = context;
    }

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
     * 转化格式
     * @param data
     * @param includedColumns
     * @param tableMapData
     * @return
     */
    protected Map<String,Serializable> convert(Serializable[] data, int[] includedColumns, ColumnsTableMapEventData tableMapData){
        Map<String, Serializable> result = new HashMap<>();
        IntStream.range(0, includedColumns.length)
                .forEach(i -> {
                    Serializable serializable = data[i];
                    if (serializable instanceof byte[]) {
                        try {
                            serializable = new String(((byte[]) serializable).clone(), StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    result.put(tableMapData.getColumnNames().get(includedColumns[i]), serializable);
                });
        return result;

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
            DataPublisher dataPublisher = context.getDataPublisher();
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

        RedissonClient redisClient = context.getRedissonClient();
        String binLogStatusKey = context.getBinaryLogConfig().getBinLogStatusKey();
        RMap<String, Object> binLogStatus = redisClient.getMap(keyPrefix(binLogStatusKey));
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

    protected String keyPrefix(String key) {

        StringBuilder builder = new StringBuilder();
        return builder
                .append(Constants.REDIS_PREFIX)
                .append(context.getBinaryLogConfig().getNamespace())
                .append("::")
                .append(key)
                .toString();
    }
}
