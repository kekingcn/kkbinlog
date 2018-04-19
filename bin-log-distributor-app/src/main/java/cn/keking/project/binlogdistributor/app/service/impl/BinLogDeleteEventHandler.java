package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import cn.keking.project.binlogdistributor.app.service.BinLogEventHandler;
import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.DeleteRowsDTO;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import com.github.shyiko.mysql.binlog.event.*;
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
public class BinLogDeleteEventHandler extends BinLogEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BinLogDeleteEventHandler.class);

    @Override
    protected EventBaseDTO formatData(Event event) {
        DeleteRowsEventData d = event.getData();
        DeleteRowsDTO deleteRowsDTO = new DeleteRowsDTO();
        deleteRowsDTO.setEventType(DatabaseEvent.DELETE_ROWS);
        //添加表信息
        ColumnsTableMapEventData tableMapData = TABLE_MAP_ID.get(d.getTableId());
        deleteRowsDTO.setDatabase(tableMapData.getDatabase());
        deleteRowsDTO.setTable(tableMapData.getTable());
        //添加列映射
        int[] includedColumns = d.getIncludedColumns().stream().toArray();
        deleteRowsDTO.setRowMaps(d.getRows().stream()
                .map(r -> convert(r,includedColumns,tableMapData)).collect(Collectors.toList()));
        return deleteRowsDTO;
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
        DeleteRowsEventData d = event.getData();
        long tableId = d.getTableId();
        TableMapEventData tableMapEventData = TABLE_MAP_ID.get(tableId);
        String tableKey = tableMapEventData.getDatabase().concat("/").concat(tableMapEventData.getTable());
        return clientInfoMap.get(tableKey);
    }
}
