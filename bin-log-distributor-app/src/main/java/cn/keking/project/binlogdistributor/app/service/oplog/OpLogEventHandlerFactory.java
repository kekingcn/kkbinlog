package cn.keking.project.binlogdistributor.app.service.oplog;

import cn.keking.project.binlogdistributor.app.service.binlog.BinLogDefaultEventHandler;
import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author: kl @kailing.pub
 * @date: 2020/1/7
 */
public class OpLogEventHandlerFactory {
    private static final Logger log = LoggerFactory.getLogger(BinLogDefaultEventHandler.class);

    private final OpLogUpdateEventHandler updateEventHandler;

    private final OpLogWriteEventHandler writeEventHandler;

    private final OpLogDeleteEventHandler deleteEventHandler;

    private final OpLogDefaultEventHandler defaultEventHandler;

    public OpLogEventHandlerFactory(OpLogEventContext context) {

        this.updateEventHandler = new OpLogUpdateEventHandler(context);
        this.writeEventHandler = new OpLogWriteEventHandler(context);
        this.deleteEventHandler = new OpLogDeleteEventHandler(context);
        this.defaultEventHandler = new OpLogDefaultEventHandler(context);
    }

    public OpLogEventHandler getHandler(String eventType) {
        switch (eventType) {
            case "u":
                return updateEventHandler;
            case "i":
                return writeEventHandler;
            case "d":
                return deleteEventHandler;
            default:
                return defaultEventHandler;
        }
    }

    public void addClientLocal(ClientInfo clientInfo) {
        switch (clientInfo.getDatabaseEvent()) {
            case WRITE_ROWS:
                writeEventHandler.addClient(clientInfo);
                log.info("添加客户端成功，参数如下：{}", clientInfo);
                break;
            case DELETE_ROWS:
                deleteEventHandler.addClient(clientInfo);
                log.info("添加客户端成功，参数如下：{}", clientInfo);
                break;
            case UPDATE_ROWS:
                updateEventHandler.addClient(clientInfo);
                log.info("添加客户端成功，参数如下：{}", clientInfo);
                break;
            default:
                log.warn("添加客户端关注变动出现未知类型，参数如下：{}", clientInfo);
        }
    }

    public void updateClientBatch(DatabaseEvent databaseEvent, List<ClientInfo> clientInfoList) {

        switch (databaseEvent) {
            case WRITE_ROWS:
                writeEventHandler.updateClientBatch(clientInfoList);
                log.info("更新客户端成功，参数如下：{}", clientInfoList);
                break;
            case DELETE_ROWS:
                deleteEventHandler.updateClientBatch(clientInfoList);
                log.info("更新客户端成功，参数如下：{}", clientInfoList);
                break;
            case UPDATE_ROWS:
                updateEventHandler.updateClientBatch(clientInfoList);
                log.info("更新客户端成功，参数如下：{}", clientInfoList);
                break;
            default:
                log.warn("更新客户端成功关注变动出现未知类型，参数如下：{}", clientInfoList);
        }
    }
}
