package cn.keking.project.binlogdistributor.app.util.leaderselector;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.service.oplog.OpLogClientFactory;
import cn.keking.project.binlogdistributor.app.service.oplog.OpLogEventHandler;
import cn.keking.project.binlogdistributor.app.service.oplog.OplogClient;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cn.keking.project.binlogdistributor.app.service.oplog.OpLogClientFactory.EVENTTYPE_KEY;

/**
 * @author: kl @kailing.pub
 * @date: 2019/8/1
 */
public class OplogLeaderSelectorListener implements LeaderSelectorListener {

    Logger logger = LoggerFactory.getLogger(getClass());
    private final OpLogClientFactory opLogClientFactory;
    private OplogClient oplogClient;
    private BinaryLogConfig binaryLogConfig;
    private Disposable disposable;
    private final BinaryLogConfigContainer binaryLogConfigContainer;

    public OplogLeaderSelectorListener(OpLogClientFactory opLogClientFactory, BinaryLogConfig binaryLogConfig, BinaryLogConfigContainer binaryLogConfigContainer) {
        this.opLogClientFactory = opLogClientFactory;
        this.binaryLogConfig = binaryLogConfig;
        this.binaryLogConfigContainer = binaryLogConfigContainer;
    }

    @Override
    public void afterTakeLeadership() {

        this.oplogClient = opLogClientFactory.initClient(binaryLogConfig);
        // 启动连接
        try {
            disposable = oplogClient.getOplog().subscribe(document -> {
                opLogClientFactory.eventCount.incrementAndGet();
                String eventType = document.getString(EVENTTYPE_KEY);
                OpLogEventHandler handler = oplogClient.getOpLogEventHandlerFactory().getHandler(eventType);
                handler.handle(document);
            });
            binaryLogConfig.setActive(true);
            binaryLogConfig.setVersion(binaryLogConfig.getVersion() + 1);
            binaryLogConfigContainer.modifyBinLogConfig(binaryLogConfig);
        } catch (Exception e) {
            // TODO: 17/01/2018 继续优化异常处理逻辑
            logger.error("[" + binaryLogConfig.getNamespace() + "] 处理事件异常，{}", e);
        }

    }

    @Override
    public boolean afterLosingLeadership() {
        disposable.dispose();
        binaryLogConfig.setActive(false);
        binaryLogConfigContainer.modifyBinLogConfig(binaryLogConfig);
        return opLogClientFactory.closeClient(oplogClient, binaryLogConfig.getNamespace());
    }
}
