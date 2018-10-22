package cn.keking.project.binlogdistributor.pub.impl;

import cn.keking.project.binlogdistributor.param.enums.LockLevel;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.*;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import org.redisson.api.RQueue;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/4:27 PM
 * @modified by
 */
public class DataPublisherRedisImpl implements DataPublisher {
    public static final String DATA = "BIN-LOG-DATA-";
    public static final String NOTIFIER = "BIN-LOG-NOTIFIER-";
    private static final Logger log = LoggerFactory.getLogger(DataPublisherRedisImpl.class);

    RedissonClient redissonClient;

    public DataPublisherRedisImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 发布信息
     *
     * @param clientInfos
     * @param data
     */
    @Override
    public void publish(Set<ClientInfo> clientInfos, EventBaseDTO data) {
        clientInfos.forEach(clientInfo -> {
            String clientId = clientInfo.getClientId();
            if (LockLevel.COLUMN.equals(clientInfo.getLockLevel())) {
                List<Map<String, Serializable>> rowMaps;
                //如果锁是列级别的特殊处理
                switch (clientInfo.getDatabaseEvent()) {
                    case UPDATE_ROWS:
                        //更新行要比对前后
                        UpdateRowsDTO udto = (UpdateRowsDTO) data;
                        List<UpdateRow> rows = udto.getRows();
                        rows.forEach(updateRow -> {
                            Map<String, Serializable> bm = updateRow.getBeforeRowMap();
                            Map<String, Serializable> am = updateRow.getAfterRowMap();
                            Serializable bCloumn = bm.get(clientInfo.getColumnName()) == null ? "NULL" : bm.get(clientInfo.getColumnName());
                            Serializable aCloumn = am.get(clientInfo.getColumnName()) == null ? "NULL" : am.get(clientInfo.getColumnName());
                            if (bCloumn.equals(aCloumn)) {
                                //如果两个一致，即变更的不是作为key的列
                                doPublish(clientId, DATA.concat(clientInfo.getKey()).concat(bCloumn.toString()), new UpdateRowsDTO(data, Arrays.asList(updateRow)));
                            } else {
                                //对老的来说是删除
                                doPublish(clientId, DATA.concat(clientInfo.getKey()).concat(bCloumn.toString()), new DeleteRowsDTO(data, Arrays.asList(bm)));
                                //对新的来说是插入
                                doPublish(clientId, DATA.concat(clientInfo.getKey()).concat(aCloumn.toString()), new WriteRowsDTO(data, Arrays.asList(am)));
                            }
                        });
                        break;
                    case WRITE_ROWS:
                        WriteRowsDTO wdto = (WriteRowsDTO) data;
                        rowMaps = wdto.getRowMaps();
                        rowMaps.forEach(r -> {
                            Serializable cn = r.get(clientInfo.getColumnName()) == null ? "NULL" : r.get(clientInfo.getColumnName());
                            doPublish(clientId, DATA.concat(clientInfo.getKey()).concat(cn.toString()), new WriteRowsDTO(data, Arrays.asList(r)));
                        });
                        break;
                    case DELETE_ROWS:
                        DeleteRowsDTO ddto = (DeleteRowsDTO) data;
                        rowMaps = ddto.getRowMaps();
                        rowMaps.forEach(r -> {
                            Serializable cn = r.get(clientInfo.getColumnName()) == null ? "NULL" : r.get(clientInfo.getColumnName());
                            doPublish(clientId, DATA.concat(clientInfo.getKey()).concat(cn.toString()), new DeleteRowsDTO(data, Arrays.asList(r)));
                        });
                        break;
                }
            } else {
                //其他级别直接发布
                doPublish(clientId, DATA.concat(clientInfo.getKey()), data);
            }
        });
    }


    private void doPublish(String clientId, String dataKey, EventBaseDTO data) {
        RQueue<EventBaseDTO> dataList = redissonClient.getQueue(dataKey);
        boolean result = dataList.offer(data);
        log.info("推送结果{}，推送信息,{}",result, data);
        String notifier = NOTIFIER.concat(clientId);
        RTopic<String> rTopic = redissonClient.getTopic(notifier);
        rTopic.publish(dataKey);
    }
}
