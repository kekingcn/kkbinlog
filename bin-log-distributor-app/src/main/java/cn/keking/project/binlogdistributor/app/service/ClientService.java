package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.param.model.ClientInfo;

import java.util.List;
import java.util.Set;

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
    void addClient(ClientInfo clientInfo);

    /**
     * 列出正常队列
     *
     * @return
     */
    Set<ClientInfo> listClient();

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
     * 重新入队或删除
     * @param uuid :uuid
     * @return
     */
    boolean enqueueOrdDlete(String uuid,String dataKey,String type,String errClient);
}
