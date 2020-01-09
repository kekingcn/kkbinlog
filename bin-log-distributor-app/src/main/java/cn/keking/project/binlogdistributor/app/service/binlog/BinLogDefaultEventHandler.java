package cn.keking.project.binlogdistributor.app.service.binlog;

import com.github.shyiko.mysql.binlog.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhenhui
 * @date Created in 2018/17/01/2018/5:28 PM
 * @modified by
 */
public class BinLogDefaultEventHandler extends BinLogEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BinLogDefaultEventHandler.class);

    public BinLogDefaultEventHandler(BinLogEventContext context) {
        super(context);
    }

    @Override
    public void handle(Event event) {
        log.debug("跳过不处理事件event:{}", event);
    }

}
