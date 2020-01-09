package cn.keking.project.binlogdistributor.param.model.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * @author zhenhui
 * @Ddate Created in 2018/19/01/2018/3:18 PM
 * @modified by
 */
public class UpdateRow implements Serializable {

    private static final long serialVersionUID = -2966621316372838979L;

    private Map<String, Object> beforeRowMap;
    private Map<String, Object> afterRowMap;

    public UpdateRow() {

    }

    public UpdateRow(Map<String, Object> beforeRowMap, Map<String, Object> afterRowMap) {
        this.beforeRowMap = beforeRowMap;
        this.afterRowMap = afterRowMap;
    }

    public Map<String, Object> getBeforeRowMap() {
        return beforeRowMap;
    }

    public void setBeforeRowMap(Map<String, Object> beforeRowMap) {
        this.beforeRowMap = beforeRowMap;
    }

    public Map<String, Object> getAfterRowMap() {
        return afterRowMap;
    }

    public void setAfterRowMap(Map<String, Object> afterRowMap) {
        this.afterRowMap = afterRowMap;
    }

    @Override
    public String toString() {
        return "UpdateRow{" +
                "beforeRowMap=" + beforeRowMap +
                ", afterRowMap=" + afterRowMap +
                '}';
    }
}
