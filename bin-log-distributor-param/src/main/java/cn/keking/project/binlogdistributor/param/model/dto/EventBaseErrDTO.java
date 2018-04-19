package cn.keking.project.binlogdistributor.param.model.dto;

import java.io.Serializable;

/**
 * @author zhenhui
 * @Ddate Created in 2018/27/01/2018/3:24 PM
 * @modified by
 */
public class EventBaseErrDTO implements Serializable {
    private EventBaseDTO eventBaseDTO;
    private Exception exception;
    private String dataKey;

    public EventBaseErrDTO() {
    }

    public EventBaseErrDTO(EventBaseDTO eventBaseDTO, Exception exception) {
        this.eventBaseDTO = eventBaseDTO;
        this.exception = exception;
    }

    public EventBaseErrDTO(EventBaseDTO eventBaseDTO, Exception exception, String dataKey) {
        this.eventBaseDTO = eventBaseDTO;
        this.exception = exception;
        this.dataKey = dataKey;
    }

    public EventBaseDTO getEventBaseDTO() {
        return eventBaseDTO;
    }

    public void setEventBaseDTO(EventBaseDTO eventBaseDTO) {
        this.eventBaseDTO = eventBaseDTO;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }
}
