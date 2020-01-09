package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.util.Result;
import cn.keking.project.binlogdistributor.param.model.ClientInfo;

import java.util.List;
import java.util.Map;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/2:44 PM
 * @modified by
 */
public interface ClientService {
    /**
     * 添加服务器端及本地client信息
     *
     * @param clientInfo
     */
    void addClient(ClientInfo clientInfo, Integer partitions, Integer replication);

    /**
     * 客户端订阅信息
     *
     * @return
     */
    List<ClientInfo> listClient(String queryType);

    /**
     * 客户端订阅信息
     *
     * @return
     */
    Map<String, List<ClientInfo>> listClientMap();

    /**
     * 列出所有应用的错误队列
     * @return
     */
    List<String> listErrorClient();

    /**
     * 删除服务器端及本地client信息
     * @param clientInfo
     */
    void deleteClient(ClientInfo clientInfo);

    /**
     * 获取日志文件状态：日志读到哪个文件的第几行
     */
    String getLogStatus();

    /**
     * 获取应用队列长度
     */
    String getqueuesize(String clientName,String type,int page);

    /**
     * 删除队列中该条记录
     * @param uuid :uuid
     * @param errClient 对列名
     * @return
     */
    boolean deleteFromQueue(String uuid,String errClient);

    /**
     * 获取所有的namespace
     */
    List<String> listNamespace();

    Result deleteTopic(String clientInfoKey);
}
