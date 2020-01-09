package cn.keking.project.binlogdistributor.app.service.oplog;

import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.DeleteRowsDTO;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: kl @kailing.pub
 * @date: 2020/1/7
 */
public class OpLogDeleteEventHandler extends OpLogEventHandler {
    private static final Logger log = LoggerFactory.getLogger(OpLogDeleteEventHandler.class);

    public OpLogDeleteEventHandler(OpLogEventContext context) {
        super(context);
    }

    @Override
    protected EventBaseDTO formatData(Document event) {
        DeleteRowsDTO deleteRowsDTO = new DeleteRowsDTO();
        deleteRowsDTO.setEventType(DatabaseEvent.DELETE_ROWS);
        //添加表信息
        deleteRowsDTO.setDatabase(super.getDataBase(event));
        deleteRowsDTO.setTable(super.getTable(event));
        deleteRowsDTO.setNamespace(context.getBinaryLogConfig().getNamespace());
        //添加列映射
        Document context = (Document) event.get(OpLogClientFactory.CONTEXT_KEY);
        List<Map<String, Object>> urs = new ArrayList<>();
        urs.add(context);
        deleteRowsDTO.setRowMaps(urs);
        return deleteRowsDTO;
    }

    @Override
    protected Set<ClientInfo> filter(Document event) {
        String database = super.getDataBase(event);
        String table = super.getTable(event);
        String tableKey = database.concat("/").concat(table);
        return clientInfoMap.get(tableKey);
    }
}
