package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.app.service.EtcdService;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.github.shyiko.mysql.binlog.event.*;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
    protected Map<String,Object> convert(Object[] data, int[] includedColumns, ColumnsTableMapEventData tableMapData){
        Map<String, Object> result = new HashMap<>();
        IntStream.range(0, includedColumns.length)
                .forEach(i -> {
                    Object serializable = data[i];
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
        String key = getClientInfoMapKey(client);
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
         String key = getClientInfoMapKey(clientInfo);
         Set<ClientInfo> clientInfos = clientInfoMap.get(key);
         clientInfos.remove(clientInfo);
         clientInfoMap.put(key, clientInfos);
     }

    public void updateClientBatch(List<ClientInfo> newClientInfoSet) {

        newClientInfoSet
                .stream()
                .collect(Collectors.groupingBy(this::getClientInfoMapKey))
                .forEach((mapKey, clientInfoList) -> clientInfoMap.put(mapKey, new HashSet<>(clientInfoList)));
    }

    private String getClientInfoMapKey(ClientInfo clientInfo) {
        return clientInfo.getDatabaseName().concat("/").concat(clientInfo.getTableName());
    }

    /**
     * 更新日志位置
     *
     * @param header
     */
    protected void updateBinaryLogStatus(EventHeaderV4 header) {

        EtcdService etcdService = context.getEtcdService();
        etcdService.updateBinLogStatus(null, header.getNextPosition(), context.getBinaryLogConfig(), header.getTimestamp());
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
