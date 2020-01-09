package cn.keking.project.binlogdistributor.app.model.enums;

public enum BinLogCommandType {

    START_DATASOURCE("START_DATASOURCE", "开启数据源"),
    STOP_DATASOURCE("STOP_DATASOURCE", "关闭数据源");

    private final String code;

    private final String description;

    BinLogCommandType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
