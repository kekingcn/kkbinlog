package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.service.*;
import cn.keking.project.binlogdistributor.app.util.Result;
import cn.keking.project.binlogdistributor.param.enums.Constants;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseErrDTO;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.http.client.domain.QueueInfo;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.JsonJacksonMapCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
    private RabbitMQService rabbitMQService;

    @Autowired
    private BinaryLogConfigContainer binaryLogConfigContainer;

    @Autowired
    private EtcdService etcdService;

    @Autowired
    private DataPublisher dataPublisher;

    @Autowired
    private KafkaService kafkaService;

    @Override
    public void addClient(ClientInfo clientInfo, Integer partitions, Integer replication) {

        etcdService.addBinLogConsumerClient(clientInfo);

        if(ClientInfo.QUEUE_TYPE_KAFKA.equals(clientInfo.getQueueType())){
            kafkaService.createKafkaTopic(clientInfo, partitions, replication);
        }

    }

    @Override
    public List<ClientInfo> listClient(String queryType) {

        return etcdService.listBinLogConsumerClient(queryType);
    }

    @Override
    public Map<String, List<ClientInfo>> listClientMap() {

        List<ClientInfo> clientInfos = listClient(null);
        if(clientInfos == null || clientInfos.isEmpty()) {
            return new HashMap<>();
        } else {
            return clientInfos.stream().collect(Collectors.groupingBy(ClientInfo::getKey));
        }
    }

    @Override
    public List<String> listErrorClient() {

        List<String> clientList=new ArrayList();
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(Constants.REDIS_PREFIX.concat("BIN-LOG-ERR-MAP-*"));
        keys.forEach(key->{
            clientList.add(key);
        });
        return clientList;
    }

    @Override
    public void deleteClient(ClientInfo clientInfo) {

        etcdService.removeBinLogConsumerClient(clientInfo);
    }

    @Override
    public String getLogStatus() {

        List<Map<String, Object>> res = etcdService.listBinLogStatus();
        return JSONArray.toJSONString(res);
    }

    @Override
    public String getqueuesize(String clientName,String type,int page) {
        int repage=10*(page-1);

        JSONObject object = new JSONObject();
        String ClientId;
        if (ClientInfo.QUEUE_TYPE_REDIS.equals(type)) {
                ClientId="BIN-LOG-DATA-".concat(clientName);
                object.put("queueSize",redissonClient.getList(ClientId).size());
                object.put("queue",redissonClient.getList(ClientId)
                        .get(repage,repage+1,repage+2,repage+3,repage +4,repage+5,repage+6,repage+7,repage+8,repage+9));
                return object.toJSONString();
        } else if (ClientInfo.QUEUE_TYPE_RABBIT.equals(type)) {
                ClientId="BIN-LOG-DATA-".concat(clientName);
                QueueInfo queueInfo = rabbitMQService.getQueue(ClientId);
                if (queueInfo == null || new Long(0L).equals(queueInfo.getMessagesReady())) {
                    object.put("queueSize", 0);
                    object.put("queue", new ArrayList<>());
                    return object.toJSONString();
                } else {
                    long queueSize = queueInfo.getMessagesReady();
                    long count = (repage + 10) > queueSize ? queueSize : (repage + 10);
                    List<EventBaseDTO> list = rabbitMQService.getMessageList(ClientId, count);
                    list = list.subList(repage, list.size());
                    object.put("queueSize", queueSize);
                    object.put("queue", list);
                    return object.toJSONString();
                }
        } else if (ClientInfo.QUEUE_TYPE_KAFKA.equals(type)) {

                // TODO 增加Kafka队列查询
                object.put("queueSize", 0);
                object.put("queue",new ArrayList<>());
                return object.toJSONString();

        } else {
                ClientId=clientName;
                object.put("queueSize",redissonClient.getMap(ClientId).size());
                RMap<String, JSONObject> map = redissonClient.getMap(ClientId,new JsonJacksonMapCodec(String.class,JSONObject.class));
                Collection<JSONObject> values = map.values();
                JSONArray array1 = new JSONArray();
                array1.addAll(values);
                object.put("queue",array1);
                return object.toJSONString();
        }
    }

    @Override
    public boolean deleteFromQueue(String uuid,String errClient) {
        RMap<String, EventBaseErrDTO> map = redissonClient.getMap(errClient, new JsonJacksonMapCodec(String.class, EventBaseErrDTO.class));
        map.remove(uuid);
        return true;
    }

    @Override
    public List<String> listNamespace() {

        return binaryLogConfigContainer.getNamespaceList();
    }

    @Override
    public Result deleteTopic(String clientInfoKey) {

        List<ClientInfo> clientInfos = etcdService.listBinLogConsumerClientByKey(clientInfoKey);

        // 从EventHandler的发送列表中删除
        etcdService.removeBinLogConsumerClient(clientInfos);

        // 刪除对应队列中的topic
        Set<String> clientInfoSet = new HashSet<>();
        Iterator<ClientInfo> iterator = clientInfos.iterator();
        while(iterator.hasNext()) {
            ClientInfo clientInfo = iterator.next();
            String identify = clientInfo.getQueueType() + clientInfo.getKey();
            if(!clientInfoSet.add(identify)) {
                iterator.remove();
            }
        }

        int deleteSum =clientInfos.size();
        int successCount = dataPublisher.deletePublishTopic(clientInfos);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("deleteSum", deleteSum);
        jsonObject.put("successCount", successCount);
        Result result = new Result(Result.SUCCESS, jsonObject.toJSONString());

        return result;
    }
}
