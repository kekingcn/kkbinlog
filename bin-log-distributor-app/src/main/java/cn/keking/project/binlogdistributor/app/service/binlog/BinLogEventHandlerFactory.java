package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.github.shyiko.mysql.binlog.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 根据类型提供事件的handler
 *
 * @author zhenhui
 * @date Created in 2018/17/01/2018/5:15 PM
 * @modified by
 */
public class BinLogEventHandlerFactory {
    private static final Logger log = LoggerFactory.getLogger(BinLogDefaultEventHandler.class);

    BinLogUpdateEventHandler  binLogUpdateEventHandler;

    BinLogWriteEventHandler binLogWriteEventHandler;

    BinLogDeleteEventHandler binLogDeleteEventHandler;

    BinLogDefaultEventHandler binLogDefaultEventHandler;

    BinLogTableMapEventHandler binLogTableMapEventHandler;

    BinLogRotateEventHandler binLogRotateEventHandler;

    BinLogDDLEventHandler binLogDDLEventHandler;

    public BinLogEventHandlerFactory(BinLogEventContext context) {

        this.binLogUpdateEventHandler = new BinLogUpdateEventHandler(context);
        this.binLogWriteEventHandler = new BinLogWriteEventHandler(context);
        this.binLogDeleteEventHandler = new BinLogDeleteEventHandler(context);
        this.binLogDefaultEventHandler = new BinLogDefaultEventHandler(context);
        this.binLogTableMapEventHandler = new BinLogTableMapEventHandler(context);
        this.binLogRotateEventHandler = new BinLogRotateEventHandler(context);
        this.binLogDDLEventHandler = new BinLogDDLEventHandler(context);
    }

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
        }else if(EventType.QUERY.equals(header.getEventType())){
            return binLogDDLEventHandler;
        } else {
            log.debug("不处理事件,{}", header);
            return binLogDefaultEventHandler;
        }
    }

    public void addClientLocal(ClientInfo clientInfo) {
        switch (clientInfo.getDatabaseEvent()) {
            case WRITE_ROWS:
                binLogWriteEventHandler.addClient(clientInfo);
                log.info("添加客户端成功，参数如下：{}", clientInfo);
                break;
            case DELETE_ROWS:
                binLogDeleteEventHandler.addClient(clientInfo);
                log.info("添加客户端成功，参数如下：{}", clientInfo);
                break;
            case UPDATE_ROWS:
                binLogUpdateEventHandler.addClient(clientInfo);
                log.info("添加客户端成功，参数如下：{}", clientInfo);
                break;
            default:
                log.warn("添加客户端关注变动出现未知类型，参数如下：{}", clientInfo);
        }
    }

    public void deleteClient(ClientInfo clientInfo){
        switch (clientInfo.getDatabaseEvent()) {
            case WRITE_ROWS:
                binLogWriteEventHandler.deleteClient(clientInfo);
                log.info("删除客户端成功，参数如下：{}", clientInfo);
                break;
            case DELETE_ROWS:
                binLogDeleteEventHandler.deleteClient(clientInfo);
                log.info("删除客户端成功，参数如下：{}", clientInfo);
                break;
            case UPDATE_ROWS:
                binLogUpdateEventHandler.deleteClient(clientInfo);
                log.info("删除客户端成功，参数如下：{}", clientInfo);
                break;
            default:
                log.warn("删除客户端关注变动出现未知类型，参数如下：{}", clientInfo);
        }
    }

    public void updateClientBatch(DatabaseEvent databaseEvent, List<ClientInfo> clientInfoList) {

        switch (databaseEvent) {
            case WRITE_ROWS:
                binLogWriteEventHandler.updateClientBatch(clientInfoList);
                log.info("更新客户端成功，参数如下：{}", clientInfoList);
                break;
            case DELETE_ROWS:
                binLogDeleteEventHandler.updateClientBatch(clientInfoList);
                log.info("更新客户端成功，参数如下：{}", clientInfoList);
                break;
            case UPDATE_ROWS:
                binLogUpdateEventHandler.updateClientBatch(clientInfoList);
                log.info("更新客户端成功，参数如下：{}", clientInfoList);
                break;
            default:
                log.warn("更新客户端成功关注变动出现未知类型，参数如下：{}", clientInfoList);
        }
    }
}
