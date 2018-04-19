package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
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
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/4:48 PM
 * @modified by
 */
@Service
public class BinLogWriteEventHandler extends BinLogEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BinLogWriteEventHandler.class);

    @Override
    protected EventBaseDTO formatData(Event event) {
        WriteRowsEventData d = event.getData();
        WriteRowsDTO writeRowsDTO = new WriteRowsDTO();
        writeRowsDTO.setEventType(DatabaseEvent.WRITE_ROWS);
        //添加表信息
        ColumnsTableMapEventData tableMapData = TABLE_MAP_ID.get(d.getTableId());
        writeRowsDTO.setDatabase(tableMapData.getDatabase());
        writeRowsDTO.setTable(tableMapData.getTable());
        //添加列映射
        int[] includedColumns = d.getIncludedColumns().stream().toArray();
        writeRowsDTO.setRowMaps(d.getRows().stream()
                .map(r -> convert(r,includedColumns,tableMapData)).collect(Collectors.toList()));
        return writeRowsDTO;
    }

    /**
     * 转化格式
     * @param data
     * @param includedColumns
     * @param tableMapData
     * @return
     */
    private Map<String,Serializable> convert(Serializable[] data, int[] includedColumns, ColumnsTableMapEventData tableMapData){
        Map<String, Serializable> result = new HashMap<>();
        IntStream.range(0, includedColumns.length)
                .forEach(i -> result.put(tableMapData.getColumnNames().get(includedColumns[i]),
                        data[i]));
        return result;

    }

    @Override
    protected Set<ClientInfo> filter(Event event) {
        WriteRowsEventData d = event.getData();
        long tableId = d.getTableId();
        TableMapEventData tableMapEventData = TABLE_MAP_ID.get(tableId);
        String tableKey = tableMapEventData.getDatabase().concat("/").concat(tableMapEventData.getTable());
        return clientInfoMap.get(tableKey);
    }
}
