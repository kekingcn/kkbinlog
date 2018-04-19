package cn.keking.project.binlogdistributor.client.service;

import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;

/**
 * @author zhenhui
 * @Ddate Created in 2018/18/01/2018/7:10 PM
 * @modified by
 */
public interface DatabaseEventHandler {
    void handle(EventBaseDTO data);
    Class getClazz();
}
