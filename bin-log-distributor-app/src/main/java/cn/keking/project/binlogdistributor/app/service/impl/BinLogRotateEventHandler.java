package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.service.BinLogEventHandler;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import org.redisson.api.RMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * RotateEvent，主要是更新文件位置
 *
 * @author zhenhui
 * @date Created in 2018/17/01/2018/5:28 PM
 * @modified by
 */
@Service
public class BinLogRotateEventHandler extends BinLogEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BinLogRotateEventHandler.class);

    @Override
    public void handle(Event event) {
        RotateEventData d = event.getData();
        RMap<String, Object> binLogStatus = redissonClient.getMap(binaryLogConfig.getBinLogStatusKey());
        binLogStatus.put("binlogFilename", d.getBinlogFilename());
        binLogStatus.put("binlogPosition", d.getBinlogPosition());
    }

}
