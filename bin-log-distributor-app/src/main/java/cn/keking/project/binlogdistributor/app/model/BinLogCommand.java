package cn.keking.project.binlogdistributor.app.model;

import cn.keking.project.binlogdistributor.app.model.enums.BinLogCommandType;

/**
 * @author wanglaomo
 * @since 2019/8/7
 **/
public class BinLogCommand {

    private String namespace;

    private String delegatedIp;

    private BinLogCommandType type;

    public BinLogCommand() {}

    public BinLogCommand(String namespace, BinLogCommandType type) {
        this.namespace = namespace;
        this.type = type;
    }

    public BinLogCommand(String namespace, String delegatedIp, BinLogCommandType type) {
        this.namespace = namespace;
        this.delegatedIp = delegatedIp;
        this.type = type;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public BinLogCommandType getType() {
        return type;
    }

    public void setType(BinLogCommandType type) {
        this.type = type;
    }

    public String getDelegatedIp() {
        return delegatedIp;
    }

    public void setDelegatedIp(String delegatedIp) {
        this.delegatedIp = delegatedIp;
    }
}
