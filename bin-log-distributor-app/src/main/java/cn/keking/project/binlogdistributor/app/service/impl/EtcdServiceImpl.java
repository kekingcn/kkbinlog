package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.model.BinLogCommand;
import cn.keking.project.binlogdistributor.app.model.ServiceStatus;
import cn.keking.project.binlogdistributor.app.service.EtcdService;
import cn.keking.project.binlogdistributor.app.util.EtcdKeyPrefixUtil;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author wanglaomo
 * @since 2019/8/6
 **/
@Service
public class EtcdServiceImpl implements EtcdService {

    private static final Logger logger = LoggerFactory.getLogger(EtcdServiceImpl.class);

    @Autowired
    private Client etcdClient;

    @Autowired
    private BinaryLogConfigContainer binaryLogConfigContainer;

    @Autowired
    private EtcdKeyPrefixUtil etcdKeyPrefixUtil;

    @Override
    public boolean sendBinLogCommand(BinLogCommand binLogCommand) {
        KV kvClient = etcdClient.getKVClient();
        try {
            kvClient.put(
                    ByteSequence.from(etcdKeyPrefixUtil.withPrefix(Constants.DEFAULT_BINLOG_CONFIG_COMMAND_KEY), StandardCharsets.UTF_8),
                    ByteSequence.from(JSON.toJSONString(binLogCommand), StandardCharsets.UTF_8)
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to connect to etcd server.",e);
            return false;
        }

        return true;
    }

    @Override
    public void updateBinLogStatus(String binlogFilename, long binlogPosition, BinaryLogConfig config, long timestamp) {

        String namespace = config.getNamespace();
        String binLogStatusKey = config.getBinLogStatusKey();

        String binLogStatusStr = getMetaData(namespace, binLogStatusKey);
        Map<String, Object> binLogStatus;
        if(StringUtils.isEmpty(binLogStatusStr)) {
            binLogStatus = new HashMap<>();
        } else {
            binLogStatus = JSON.parseObject(binLogStatusStr).toJavaObject(Map.class);
        }

        if(!StringUtils.isEmpty(binlogFilename)) {
            binLogStatus.put("binlogFilename", binlogFilename);
        }
        binLogStatus.put("binlogPosition", binlogPosition);
        binLogStatus.put("timestamp", timestamp);
        binLogStatus.put("datetime", LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_DATE_TIME));
        writeMetaData(namespace, binLogStatusKey, JSON.toJSONString(binLogStatus));
    }

    @Override
    public List<Map<String, Object>> listBinLogStatus() {

        List<Map<String, Object>> res = binaryLogConfigContainer
                .getConfigs()
                .stream()
                .map(config -> {
                    String binLogStatusStr = getMetaData(config.getNamespace(), config.getBinLogStatusKey());
                    // binLogStatusStr shouldn't be null
                    Map<String, Object> map = JSON.parseObject(binLogStatusStr).toJavaObject(Map.class);
                    map.put("namespace", config.getNamespace());
                    return map;
                })
                .collect(Collectors.toList());

        return res;
    }

    @Override
    public JSONObject getBinaryLogStatus(BinaryLogConfig binaryLogConfig) {

        String namespace = binaryLogConfig.getNamespace();
        String binLogStatusKey = binaryLogConfig.getBinLogStatusKey();

        String binLogStatusStr = getMetaData(namespace, binLogStatusKey);
        if(StringUtils.isEmpty(binLogStatusStr)) {
            return null;
        }
        return JSON.parseObject(binLogStatusStr);
    }

    @Override
    public void addBinLogConsumerClient(ClientInfo clientInfo) {
        String namespace = clientInfo.getNamespace();
        BinaryLogConfig config = binaryLogConfigContainer.getConfigByNamespace(namespace);

        String binLogClientSetStr = getMetaData(namespace, config.getBinLogClientSet());
        Set<ClientInfo> clientSet = null;
        if(StringUtils.isEmpty(binLogClientSetStr)) {
            clientSet = new HashSet<>();
        } else {
            List<ClientInfo> clientList = JSON.parseArray(binLogClientSetStr, ClientInfo.class);
            clientSet = new HashSet<>(clientList);
        }
        clientSet.add(clientInfo);
        writeMetaData(namespace, config.getBinLogClientSet(), JSON.toJSONString(clientSet));
    }

    @Override
    public void removeBinLogConsumerClient(ClientInfo clientInfo) {

        String namespace = clientInfo.getNamespace();

        BinaryLogConfig config = binaryLogConfigContainer.getConfigByNamespace(namespace);
        String binLogClientSetStr = getMetaData(namespace, config.getBinLogClientSet());
        if(StringUtils.isEmpty(binLogClientSetStr)) {
            return;
        } else {
            List<ClientInfo> clientList = JSON.parseArray(binLogClientSetStr, ClientInfo.class);
            Set<ClientInfo> clientSet = new HashSet<>(clientList);
            clientSet.remove(clientInfo);
            writeMetaData(namespace, config.getBinLogClientSet(), JSON.toJSONString(clientSet));
        }
    }

    @Override
    public void removeBinLogConsumerClient(List<ClientInfo> clientInfos) {

        Map<String, List<ClientInfo>> clientMap = clientInfos.stream().collect(Collectors.groupingBy(ClientInfo::getNamespace));
        clientMap.forEach((namespace, clientList) -> {

            BinaryLogConfig config = binaryLogConfigContainer.getConfigByNamespace(namespace);
            String binLogClientSetStr = getMetaData(namespace, config.getBinLogClientSet());
            if(StringUtils.isEmpty(binLogClientSetStr)) {
                return;
            } else {
                List<ClientInfo> currentList = JSON.parseArray(binLogClientSetStr, ClientInfo.class);
                Set<ClientInfo> clientSet = new HashSet<>(currentList);
                clientSet.removeAll(clientList);
                writeMetaData(namespace, config.getBinLogClientSet(), JSON.toJSONString(clientSet));
            }
        });

    }

    @Override
    public List<ClientInfo> listBinLogConsumerClient(BinaryLogConfig binaryLogConfig) {

        String namespace = binaryLogConfig.getNamespace();
        String binLogClientSetKey = binaryLogConfig.getBinLogClientSet();
        String binLogClientSetStr = getMetaData(namespace, binLogClientSetKey);

        if(StringUtils.isEmpty(binLogClientSetStr)) {
            return new ArrayList<>();
        }
        List<ClientInfo> clientSet = JSON.parseArray(binLogClientSetStr, ClientInfo.class);
        return clientSet;
    }

    @Override
    public List<ClientInfo> listBinLogConsumerClientByKey(String clientInfoKey) {

        List<ClientInfo> clientInfos = listBinLogConsumerClient((String) null);
        if(clientInfos == null || clientInfos.isEmpty()) {
            return clientInfos;
        }

        return clientInfos
                .stream()
                .filter(clientInfo -> clientInfo.getKey().equals(clientInfoKey))
                .collect(Collectors.toList());
    }


    @Override
    public List<ClientInfo> listBinLogConsumerClient(String queryType) {
        return binaryLogConfigContainer
                .getConfigs()
                .stream()
                .map(config -> getMetaData(config.getNamespace(), config.getBinLogClientSet()))
                .map(str -> {
                    List<ClientInfo> clientInfos = JSON.parseArray(str, ClientInfo.class);
                    return clientInfos == null ? new ArrayList<ClientInfo>() : clientInfos;
                })
                .flatMap(List::stream)
                .filter(clientInfo -> queryType == null ? true : queryType.equals(clientInfo.getQueueType()))
                .sorted(Comparator.comparing(ClientInfo::getNamespace))
                .collect(Collectors.toList());
    }

    @Override
    public long getLease(String leaseName, long leaseTTL) throws Exception{

        Lease lease = etcdClient.getLeaseClient();
        long leaseId = lease.grant(leaseTTL).get().getID();

        lease.keepAlive(leaseId, new StreamObserver<LeaseKeepAliveResponse>() {
            @Override
            public void onNext(LeaseKeepAliveResponse value) {
                logger.debug("{} lease keeps alive for [{}]s:", leaseName, value.getTTL());
            }

            @Override
            public void onError(Throwable t) {
                logger.debug("{} lease renewal Exception!", leaseName, t.fillInStackTrace());
            }

            @Override
            public void onCompleted() {
                logger.info("{} lease completed!", leaseName);
            }
        });

        return leaseId;
    }

    @Override
    public void updateServiceStatus(String serviceKey, ServiceStatus status, long leaseId) throws Exception {

        KV client = etcdClient.getKVClient();
        client.put(ByteSequence.from(etcdKeyPrefixUtil.withPrefix(Constants.SERVICE_STATUS_PATH).concat(serviceKey), StandardCharsets.UTF_8),
                ByteSequence.from(JSON.toJSONString(status), StandardCharsets.UTF_8),
                PutOption.newBuilder().withLeaseId(leaseId).build())
                .get();
    }

    /**
     * @return
     */
    @Override
    public List<ServiceStatus> getServiceStatus() {

        KV client = etcdClient.getKVClient();
        List<ServiceStatus> serviceStatuses = null;
        try {

            ByteSequence prefix = ByteSequence.from(etcdKeyPrefixUtil.withPrefix(Constants.SERVICE_STATUS_PATH), StandardCharsets.UTF_8);

            serviceStatuses = client.get(prefix,GetOption.newBuilder().withPrefix(prefix).build())
                    .get().getKvs()
                    .stream()
                    .map(kv -> JSON.parseObject(
                            new String(kv.getValue().getBytes()),
                            ServiceStatus.class)
                    )
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            logger.warn("Get service status error.", e);
        }

        return serviceStatuses;
    }

    private String getMetaData(String namespace, String dataKey) {
        KV kvClient = etcdClient.getKVClient();
        String metaData = null;
        try {
            List<KeyValue> kvs = kvClient.get(keyPrefix(namespace, dataKey)).get().getKvs();
            if(kvs != null && !kvs.isEmpty()) {
                metaData = kvs.get(0).getValue().toString(StandardCharsets.UTF_8);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to connect to etcd server.",e);
        }
        return metaData;
    }

    private void writeMetaData(String namespace, String dataKey, String metaData) {

        KV kvClient = etcdClient.getKVClient();
        try {
            kvClient.put(keyPrefix(namespace, dataKey), ByteSequence.from(metaData, StandardCharsets.UTF_8)).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to connect to etcd server.",e);
        }
    }

    private ByteSequence keyPrefix(String namespace, String key) {

        StringBuilder builder = new StringBuilder();

        String finalKey = builder
                .append(etcdKeyPrefixUtil.withPrefix(namespace))
                .append(Constants.PATH_SEPARATOR)
                .append(key)
                .toString();

        return ByteSequence.from(finalKey, StandardCharsets.UTF_8);
    }
}
