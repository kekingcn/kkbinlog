package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.model.BinLogCommand;
import cn.keking.project.binlogdistributor.app.model.ServiceStatus;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author wanglaomo
 * @since 2019/8/6
 **/
public interface EtcdService {

    boolean sendBinLogCommand(BinLogCommand binLogCommand);

    void updateBinLogStatus(String binlogFilename, long binlogPosition, BinaryLogConfig binaryLogConfig, long timestamp);

    List<Map<String, Object>> listBinLogStatus();

    void addBinLogConsumerClient(ClientInfo clientInfo);

    List<ClientInfo> listBinLogConsumerClient(String queryType);

    void removeBinLogConsumerClient(ClientInfo clientInfo);

    List<ClientInfo> listBinLogConsumerClient(BinaryLogConfig binaryLogConfig);

    JSONObject getBinaryLogStatus(BinaryLogConfig binaryLogConfig);

    List<ClientInfo> listBinLogConsumerClientByKey(String clientInfoKey);

    void removeBinLogConsumerClient(List<ClientInfo> clientInfos);

    long getLease(String leaseName, long leaseTTL) throws Exception;

    void updateServiceStatus(String serviceKey, ServiceStatus status, long leaseId) throws Exception;

    List<ServiceStatus> getServiceStatus();
}
