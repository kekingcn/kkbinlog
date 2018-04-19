package cn.keking.project.binlogdistributor.client.service.sub;

import cn.keking.project.binlogdistributor.client.BinLogDistributorClient;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/4:26 PM
 * @modified by
 */
public interface DataSubscriber {
    void subscribe(String clientId, BinLogDistributorClient binLogDistributorClient);
}
