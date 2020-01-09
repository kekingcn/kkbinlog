package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.param.model.dto.UpdateRow;
import cn.keking.project.binlogdistributor.param.model.dto.UpdateRowsDTO;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/4:48 PM
 * @modified by
 */
public class BinLogUpdateEventHandler extends BinLogEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BinLogUpdateEventHandler.class);

    public BinLogUpdateEventHandler(BinLogEventContext context) {
        super(context);
    }

    @Override
    protected EventBaseDTO formatData(Event event) {
        UpdateRowsEventData d = event.getData();
        UpdateRowsDTO updateRowsDTO = new UpdateRowsDTO();
        updateRowsDTO.setEventType(DatabaseEvent.UPDATE_ROWS);
        //添加表信息
        ColumnsTableMapEventData tableMapData = context.getTableMapData(d.getTableId());
        updateRowsDTO.setDatabase(tableMapData.getDatabase());
        updateRowsDTO.setTable(tableMapData.getTable());
        updateRowsDTO.setNamespace(context.getBinaryLogConfig().getNamespace());
        //添加列映射
        int[] includedColumns = d.getIncludedColumns().stream().toArray();
        List<UpdateRow> urs = d.getRows().stream()
                .map(e -> new UpdateRow(convert(e.getKey(),includedColumns,tableMapData),
                        convert(e.getValue(),includedColumns,tableMapData))).collect(Collectors.toList());
        updateRowsDTO.setRows(urs);
        return updateRowsDTO;
    }

    @Override
    protected Set<ClientInfo> filter(Event event) {
        UpdateRowsEventData d = event.getData();
        long tableId = d.getTableId();
        TableMapEventData tableMapEventData =  context.getTableMapData(tableId);
        String tableKey = tableMapEventData.getDatabase().concat("/").concat(tableMapEventData.getTable());
        return clientInfoMap.get(tableKey);
    }
}
