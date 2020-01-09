package cn.keking.project.binlogdistributor.param.enums;

/**
 * @author wanglaomo
 * @since 2019/6/4
 **/
public interface Constants {

    String REDIS_PREFIX = "BINLOG::DISTRIBUTOR::";

    String PATH_SEPARATOR = "/";

    String DEFAULT_ETCD_METADATA_PREFIX = "BINLOG-DISTRIBUTOR" + PATH_SEPARATOR;

    String DEFAULT_BINLOG_CONFIG_COMMAND_KEY = "CONFIG_COMMAND";

    String DEFAULT_BINLOG_CONFIG_KEY = "DATASOURCE-CONFIG";

    String LEADER_IDENTIFICATION_PATH = "leader-selector/leader-identification/";

    String SERVICE_STATUS_PATH        =  "service-status/";
}
