package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.service.BinLogEventHandlerFactory;
import cn.keking.project.binlogdistributor.app.service.ClientService;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseErrDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.codec.JsonJacksonMapCodec;
import org.redisson.client.codec.MapScanCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.codec.SerializationCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/2:46 PM
 * @modified by
 */
@Service
public class ClientServiceRedisImpl implements ClientService {

    @Autowired
    RedissonClient redissonClient;
    @Autowired
    BinaryLogConfig binaryLogConfig;
    @Autowired
    BinLogEventHandlerFactory binLogEventHandlerFactory;

    @Override
    public void addClient(ClientInfo clientInfo) {
        RSet<ClientInfo> clientSet = redissonClient.getSet(binaryLogConfig.getBinLogClientSet());
        clientSet.add(clientInfo);
        binLogEventHandlerFactory.addClientLocal(clientInfo);
    }

    @Override
    public Set<ClientInfo> listClient() {
        RSet<ClientInfo> clientSet = redissonClient.getSet(binaryLogConfig.getBinLogClientSet());
        Set<ClientInfo> result = new HashSet<>();
        clientSet.forEach(clientInfo -> {
            result.add(clientInfo);
        });
            return clientSet;

    }

    @Override
    public List<String> listErrorClient() {
        List<String> clientList=new ArrayList();
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("BIN-LOG-ERR-MAP-".concat("*"));
        keys.forEach(key->{
            clientList.add(key);
        });
        return clientList;
    }

    @Override
    public void deleteClient(ClientInfo clientInfo) {
        RSet<ClientInfo> clientSet = redissonClient.getSet(binaryLogConfig.getBinLogClientSet());
        clientSet.remove(clientInfo);
        binLogEventHandlerFactory.deleteClient(clientInfo);
    }

    @Override
    public String getLogStatus() {
        RMap<String, Object> map = redissonClient.getMap(binaryLogConfig.getBinLogStatusKey());
        JSONObject object = new JSONObject();
        object.putAll(map);
        return object.toJSONString();
    }

    @Override
    public String getqueuesize(String clientName,String type,int page) {
        int repage=10*(page-1);
        JSONObject object = new JSONObject();
        String ClientId;
            if(type.equals("info")){
                ClientId="BIN-LOG-DATA-".concat(clientName);
                object.put("queueSize",redissonClient.getList(ClientId).size());
                object.put("queue",redissonClient.getList(ClientId)
                        .get(repage,repage+1,repage+2,repage+3,repage +4,repage+5,repage+6,repage+7,repage+8,repage+9));
                return object.toJSONString();
            }else {
                ClientId=clientName;
                object.put("queueSize",redissonClient.getMap(ClientId).size());
                RMap<String, EventBaseErrDTO> map = redissonClient.getMap(ClientId,new JsonJacksonMapCodec(String.class,EventBaseErrDTO.class));
                JSONArray array = new JSONArray();
                map.keySet().forEach(key->{
                    JSONObject temp = new JSONObject();
                    EventBaseErrDTO eventBaseErrDTO = map.get(key);
                    temp.put("exception",eventBaseErrDTO.getException().getMessage());
                    temp.put("dataKey", eventBaseErrDTO.getDataKey());
                    temp.put("uuid", key);
                    temp.put("eventType", eventBaseErrDTO.getEventBaseDTO().getEventType());
                    temp.put("dataBase", eventBaseErrDTO.getEventBaseDTO().getDatabase());
                    temp.put("table", eventBaseErrDTO.getEventBaseDTO().getTable());
                    array.add(temp);
                });
                object.put("queue",array);
                return object.toJSONString();
            }
    }

    @Override
    public boolean enqueueOrdDlete(String uuid,String dataKey,String type,String errClient) {
        RMap<String, EventBaseErrDTO> map = redissonClient.getMap(errClient, new JsonJacksonMapCodec(String.class, EventBaseErrDTO.class));
        EventBaseErrDTO eventBaseErrDTO = map.get(uuid);
        if(type.equals("delete")){
            map.remove(uuid);
            return  true;
        }else {
            EventBaseDTO eventBaseDTO = eventBaseErrDTO.getEventBaseDTO();
            boolean addResult = redissonClient.getQueue(dataKey).add(eventBaseDTO);
            map.remove(uuid);
            return addResult;
        }
    }
}
