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
public class DeleteRowsDTO extends EventBaseDTO implements Serializable {

    private static final long serialVersionUID = -958814764356190600L;

    private List<Map<String, Object>> rowMaps;

    public DeleteRowsDTO() {
    }

    public DeleteRowsDTO(EventBaseDTO eventBaseDTO, List<Map<String, Object>> rowMaps) {
        super(eventBaseDTO);
        super.setEventType(DatabaseEvent.DELETE_ROWS);
        this.rowMaps = rowMaps;
    }

    public List<Map<String, Object>> getRowMaps() {
        return rowMaps;
    }

    public void setRowMaps(List<Map<String, Object>> rowMaps) {
        this.rowMaps = rowMaps;
    }

    @Override
    public String toString() {
        return "DeleteRowsDTO{" +
                "rowMaps=" + rowMaps +
                "} " + super.toString();
    }
}
