package cn.keking.project.binlogdistributor.param.model.dto;

import cn.keking.project.binlogdistributor.param.enums.DatabaseEvent;

import java.util.List;

/**
 * @author zhenhui
 * @Ddate Created in 2018/19/01/2018/3:18 PM
 * @modified by
 */
public class UpdateRowsDTO extends EventBaseDTO {
    private List<UpdateRow> rows;

    public UpdateRowsDTO() {
    }

    public UpdateRowsDTO(EventBaseDTO eventBaseDTO, List<UpdateRow> rows) {
        super(eventBaseDTO);
        super.setEventType(DatabaseEvent.UPDATE_ROWS);
        this.rows = rows;
    }

    public List<UpdateRow> getRows() {
        return rows;
    }

    public void setRows(List<UpdateRow> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "UpdateRowsDTO{" +
                "rows=" + rows +
                "} " + super.toString();
    }
}
