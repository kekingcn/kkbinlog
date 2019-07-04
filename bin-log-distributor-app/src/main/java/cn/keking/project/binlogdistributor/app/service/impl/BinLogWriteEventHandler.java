package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.app.service.BinLogEventContext;
import cn.keking.project.binlogdistributor.app.service.BinLogEventHandler;
import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.param.model.dto.WriteRowsDTO;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/4:48 PM
 * @modified by
 */
public class BinLogWriteEventHandler extends BinLogEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BinLogWriteEventHandler.class);

    public BinLogWriteEventHandler(BinLogEventContext context) {
        super(context);
    }

    @Override
    protected EventBaseDTO formatData(Event event) {
        WriteRowsEventData d = event.getData();
        WriteRowsDTO writeRowsDTO = new WriteRowsDTO();
        writeRowsDTO.setEventType(DatabaseEvent.WRITE_ROWS);
        //添加表信息
        ColumnsTableMapEventData tableMapData = context.getTableMapData(d.getTableId());
        writeRowsDTO.setDatabase(tableMapData.getDatabase());
        writeRowsDTO.setTable(tableMapData.getTable());
        writeRowsDTO.setNamespace(context.getBinaryLogConfig().getNamespace());
        //添加列映射
        int[] includedColumns = d.getIncludedColumns().stream().toArray();
        writeRowsDTO.setRowMaps(d.getRows().stream()
                .map(r -> convert(r,includedColumns,tableMapData)).collect(Collectors.toList()));
        return writeRowsDTO;
    }
    @Override
    protected Set<ClientInfo> filter(Event event) {
        WriteRowsEventData d = event.getData();
        long tableId = d.getTableId();
        TableMapEventData tableMapEventData = context.getTableMapData(tableId);
        String tableKey = tableMapEventData.getDatabase().concat("/").concat(tableMapEventData.getTable());
        return clientInfoMap.get(tableKey);
    }
}
