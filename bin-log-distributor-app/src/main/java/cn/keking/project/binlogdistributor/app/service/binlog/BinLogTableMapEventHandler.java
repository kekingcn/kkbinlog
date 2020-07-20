package cn.keking.project.binlogdistributor.app.service.binlog;

import cn.keking.project.binlogdistributor.app.model.ColumnsTableMapEventData;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 处理TableMapEvent，主要是映射表名和id
 *
 * @author zhenhui
 * @date Created in 2018/17/01/2018/5:28 PM
 * @modified by
 */
public class BinLogTableMapEventHandler extends BinLogEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BinLogTableMapEventHandler.class);

    public BinLogTableMapEventHandler(BinLogEventContext context) {
        super(context);
    }

    @Override
    public void handle(Event event) {
        TableMapEventData d = event.getData();
        log.debug("TableMapEventData:{}", d);
        ColumnsTableMapEventData tableMapEventData = context.getTableMapData(d.getTableId());
        //如果表结构有变化，重新设置映射信息
        if (tableMapEventData == null || !ColumnsTableMapEventData.checkEqual(d, tableMapEventData)) {
            log.info("更新表映射：{}-{}", d.getDatabase(), d.getTable());
            ColumnsTableMapEventData data = new ColumnsTableMapEventData(d);
            String sql = "show columns from `" + d.getTable() + "` from `" + d.getDatabase() + "`";
            try (Connection conn = context.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet resultSet = ps.executeQuery();) {
                while (resultSet.next()) {
                    data.addColumnName(resultSet.getString("Field"));
                }
                //将表id和表映射
                context.addTableMapData(d.getTableId(), data);
            } catch (SQLException e) {
                log.error("获取表数据错误,sql语句为{}，异常如下:{}", sql, e);
            }
        }
    }
}
