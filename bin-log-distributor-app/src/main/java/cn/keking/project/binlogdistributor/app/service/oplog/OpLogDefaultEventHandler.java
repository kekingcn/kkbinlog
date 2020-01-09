package cn.keking.project.binlogdistributor.app.service.oplog;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: kl @kailing.pub
 * @date: 2020/1/7
 */
public class OpLogDefaultEventHandler extends OpLogEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OpLogDefaultEventHandler.class);

    public OpLogDefaultEventHandler(OpLogEventContext context) {
        super(context);
    }

    @Override
    public void handle(Document event) {
        log.debug("跳过不处理的MongoDB事件event:{}", event);
    }

}
