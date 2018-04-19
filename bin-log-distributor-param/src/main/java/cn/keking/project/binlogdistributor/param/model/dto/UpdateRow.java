package cn.keking.project.binlogdistributor.param.model.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * @author zhenhui
 * @Ddate Created in 2018/19/01/2018/3:18 PM
 * @modified by
 */
public class UpdateRow implements Serializable {
    private Map<String, Serializable> beforeRowMap;
    private Map<String, Serializable> afterRowMap;

    public UpdateRow() {

    }

    public UpdateRow(Map<String, Serializable> beforeRowMap, Map<String, Serializable> afterRowMap) {
        this.beforeRowMap = beforeRowMap;
        this.afterRowMap = afterRowMap;
    }

    public Map<String, Serializable> getBeforeRowMap() {
        return beforeRowMap;
    }

    public void setBeforeRowMap(Map<String, Serializable> beforeRowMap) {
        this.beforeRowMap = beforeRowMap;
    }

    public Map<String, Serializable> getAfterRowMap() {
        return afterRowMap;
    }

    public void setAfterRowMap(Map<String, Serializable> afterRowMap) {
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
