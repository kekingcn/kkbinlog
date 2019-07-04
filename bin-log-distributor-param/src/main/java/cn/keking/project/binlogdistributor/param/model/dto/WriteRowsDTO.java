package cn.keking.project.binlogdistributor.param.model.dto;

import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author zhenhui
 * @Ddate Created in 2018/19/01/2018/3:18 PM
 * @modified by
 */
public class WriteRowsDTO extends EventBaseDTO {

    private static final long serialVersionUID = 6443935897277661139L;

    private List<Map<String, Serializable>> rowMaps;

    public WriteRowsDTO() {
    }

    public WriteRowsDTO(EventBaseDTO eventBaseDTO, List<Map<String, Serializable>> rowMaps) {
        super(eventBaseDTO);
        super.setEventType(DatabaseEvent.WRITE_ROWS);
        this.rowMaps = rowMaps;
    }

    public List<Map<String, Serializable>> getRowMaps() {
        return rowMaps;
    }

    public void setRowMaps(List<Map<String, Serializable>> rowMaps) {
        this.rowMaps = rowMaps;
    }

    @Override
    public String toString() {
        return "WriteRowsDTO{" +
                "rowMaps=" + rowMaps +
                "} " + super.toString();
    }
}
