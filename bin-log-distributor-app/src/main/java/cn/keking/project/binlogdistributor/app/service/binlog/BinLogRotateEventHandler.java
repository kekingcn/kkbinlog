package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.app.service.EtcdService;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RotateEvent，主要是更新文件位置
 *
 * @author zhenhui
 * @date Created in 2018/17/01/2018/5:28 PM
 * @modified by
 */
public class BinLogRotateEventHandler extends BinLogEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BinLogRotateEventHandler.class);

    public BinLogRotateEventHandler(BinLogEventContext context) {
        super(context);
    }

    @Override
    public void handle(Event event) {
        RotateEventData d = event.getData();
        EtcdService etcdService = context.getEtcdService();
        etcdService.updateBinLogStatus(d.getBinlogFilename(), d.getBinlogPosition(), context.getBinaryLogConfig(), System.currentTimeMillis()); // event.header.timestamp = 0
    }

}
