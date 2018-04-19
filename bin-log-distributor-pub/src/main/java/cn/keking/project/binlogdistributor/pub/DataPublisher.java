package cn.keking.project.binlogdistributor.pub;

import cn.keking.project.binlogdistributor.param.model.ClientInfo;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;

import java.util.Set;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/4:26 PM
 * @modified by
 */
public interface DataPublisher {
    void publish(Set<ClientInfo> clientInfos, EventBaseDTO data);
}
