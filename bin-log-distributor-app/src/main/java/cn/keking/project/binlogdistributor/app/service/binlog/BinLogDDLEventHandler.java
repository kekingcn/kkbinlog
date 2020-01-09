package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.pub.DataPublisher;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: kl @kailing.pub
 * @date: 2019/10/28
 * DDL事件处理
 */
public class BinLogDDLEventHandler extends BinLogEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BinLogDDLEventHandler.class);
    private static final String SUBSCRIBE_SQL_EVENT = "ALTER TABLE";
    private static final String DEFAULT_TOPIC_KEY = "binlog.ddl.topic";
    private static final String DDLEVENT_ENABLE_KEY = "binlog.ddlevent.enable";
    private static final String DEFAULT_TOPIC = "binlog-ddl-topic";
    private static final String DDLEVENT_ENABLE = "false";
    private DataPublisher dataPublisher = context.getDataPublisher();

    public BinLogDDLEventHandler(BinLogEventContext context) {
        super(context);
    }

    @Override
    public void handle(Event event) {
        boolean ddleventEnable = Boolean.parseBoolean(System.getProperty(DDLEVENT_ENABLE_KEY,DDLEVENT_ENABLE));
        if(ddleventEnable){
            QueryEventData data = event.getData();
            String sql = data.getSql();
            if (sql.contains(SUBSCRIBE_SQL_EVENT)) {
                log.info("数据库：{}发生alter table事件", data.getDatabase());
                String topic = System.getProperty(DEFAULT_TOPIC_KEY,DEFAULT_TOPIC);
                dataPublisher.pushToKafka(topic, data);
            }
        }
        updateBinaryLogStatus(event.getHeader());
    }

}
