package cn.keking.project.binlogdistributor.app.util.leaderselector;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.service.binlog.BinLogClientFactory;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: kl @kailing.pub
 * @date: 2019/8/1
 */
public class BinlogLeaderSelectorListener implements LeaderSelectorListener {

    Logger logger = LoggerFactory.getLogger(getClass());
    private BinLogClientFactory binLogClientFactory;
    private BinaryLogClient binaryLogClient;
    private BinaryLogConfig binaryLogConfig;

    public BinlogLeaderSelectorListener(BinLogClientFactory binLogClientFactory, BinaryLogConfig binaryLogConfig) {
        this.binLogClientFactory = binLogClientFactory;
        this.binaryLogConfig = binaryLogConfig;
    }

    @Override
    public void afterTakeLeadership() {

        this.binaryLogClient = binLogClientFactory.initClient(binaryLogConfig);
        // 启动连接
        try {
            binaryLogClient.connect();
        } catch (Exception e) {
            // TODO: 17/01/2018 继续优化异常处理逻辑
            logger.error("[" + binaryLogConfig.getNamespace() + "] 处理事件异常，{}", e);
        }

    }

    @Override
    public boolean afterLosingLeadership() {

        return binLogClientFactory.closeClient(binaryLogClient, binaryLogConfig.getNamespace());
    }
}
