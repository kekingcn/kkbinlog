package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.service.BinLogEventHandler;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/5:28 PM
 * @modified by
 */
@Service
public class BinLogDefaultEventHandler extends BinLogEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BinLogDefaultEventHandler.class);

    @Override
    public void handle(Event event) {
        log.debug("跳过不处理事件event:{}", event);
    }

}
