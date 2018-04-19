package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.service.impl.*;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.github.shyiko.mysql.binlog.event.EventType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 根据类型提供事件的handler
 *
 * @author zhenhui
 * @date Created in 2018/17/01/2018/5:15 PM
 * @modified by
 */
@Service
public class BinLogEventHandlerFactory {
    private static final Logger log = LoggerFactory.getLogger(BinLogDefaultEventHandler.class);

    @Autowired
    BinLogUpdateEventHandler binLogUpdateEventHandler;
    @Autowired
    BinLogWriteEventHandler binLogWriteEventHandler;
    @Autowired
    BinLogDeleteEventHandler binLogDeleteEventHandler;
    @Autowired
    BinLogDefaultEventHandler binLogDefaultEventHandler;
    @Autowired
    BinLogTableMapEventHandler binLogTableMapEventHandler;
    @Autowired
    BinLogRotateEventHandler binLogRotateEventHandler;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    BinaryLogConfig binaryLogConfig;

    public BinLogEventHandler getHandler(EventHeader header) {
        //考虑到状态映射的问题，只在增删改是更新位置
        if (EventType.isUpdate(header.getEventType())) {
            return binLogUpdateEventHandler;
        } else if (EventType.isWrite(header.getEventType())) {
            return binLogWriteEventHandler;
        } else if (EventType.isDelete(header.getEventType())) {
            return binLogDeleteEventHandler;
        } else if (EventType.TABLE_MAP.equals(header.getEventType())) {
            log.debug("TableMapEvent-header:{}", header);
            return binLogTableMapEventHandler;
        } else if (EventType.ROTATE.equals(header.getEventType())) {
            log.debug("RotateEvent-header:{}", header);
            return binLogRotateEventHandler;
        } else {
            log.debug("不处理事件,{}", header);
            return binLogDefaultEventHandler;
        }
    }

    public void addClientLocal(ClientInfo clientInfo) {
        switch (clientInfo.getDatabaseEvent()) {
            case WRITE_ROWS:
                binLogWriteEventHandler.addClient(clientInfo);
                break;
            case DELETE_ROWS:
                binLogDeleteEventHandler.addClient(clientInfo);
                break;
            case UPDATE_ROWS:
                binLogUpdateEventHandler.addClient(clientInfo);
                break;
            default:
                log.warn("添加客户端关注变动出现未知类型，参数如下：{}", clientInfo);
        }
    }

    public  void deleteClient(ClientInfo clientInfo){
        switch (clientInfo.getDatabaseEvent()) {
            case WRITE_ROWS:
                binLogWriteEventHandler.deleteClient(clientInfo);
                break;
            case DELETE_ROWS:
                binLogDeleteEventHandler.deleteClient(clientInfo);
                break;
            case UPDATE_ROWS:
                binLogUpdateEventHandler.deleteClient(clientInfo);
                break;
            default:
                log.warn("删除客户端关注变动出现未知类型，参数如下：{}", clientInfo);
        }
    }
}
