package cn.keking.project.binlogdistributor.app.service.impl;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.model.vo.BinaryLogConfigVO;
import cn.keking.project.binlogdistributor.app.service.BinLogClientFactory;
import cn.keking.project.binlogdistributor.app.service.DistributorService;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author wanglaomo
 * @since 2019/6/10
 **/
@Service
public class DistributorServiceImpl implements DistributorService {

    private static final Logger logger = LoggerFactory.getLogger(DistributorServiceImpl.class);

    @Autowired
    private BinaryLogConfigContainer binaryLogConfigContainer;

    @Autowired
    private BinLogClientFactory binLogClientFactory;

    private static ExecutorService executorService;

    public void startDistribute() {

        List<BinaryLogConfig> configList = binaryLogConfigContainer.initAllConfigs();

        executorService = Executors.newFixedThreadPool(5);
        configList.forEach( config -> {
            // 在线程中启动事件监听
            executorService.submit(() -> {
                binLogDistributeTask(config, null, null);
            });
        });
    }

    private void binLogDistributeTask(BinaryLogConfig binaryLogConfig, Lock lock, Condition startCondition) {

        if(binaryLogConfig.isActive()) {
            return;
        }

        String namespace = binaryLogConfig.getNamespace();
        final BinaryLogClient client = binLogClientFactory.initClient(binaryLogConfig, lock, startCondition);
        // 启动连接
        try {
            client.connect();
        } catch (Exception e) {
            // TODO: 17/01/2018 继续优化异常处理逻辑
            logger.error("[" + namespace + "] 处理事件异常，{}", e);
        }
    }

    @Override
    public List<BinaryLogConfigVO> getAllConfigs() {

        return binaryLogConfigContainer.getConfigs().stream().map(config -> new BinaryLogConfigVO(config)).collect(Collectors.toList());
    }


    @Override
    public boolean persistDatasourceConfig(BinaryLogConfig config) {

        // 持久化
        boolean res = binaryLogConfigContainer.persistConfig(config);
        if(!res) {
            return false;
        }

        return true;
    }

    @Override
    public boolean removeDatasourceConfig(String namespace) {

        return binaryLogConfigContainer.removeConfig(namespace);
    }

    @Override
    public boolean startDatasource(String namespace) {

        if(StringUtils.isBlank(namespace)) {
            return false;
        }

        BinaryLogConfig config = binaryLogConfigContainer.getConfigByNamespace(namespace);
        Lock lock = new ReentrantLock();
        Condition startCondition = lock.newCondition();
        executorService.submit(() -> binLogDistributeTask(config, lock, startCondition));

        lock.lock();
        try {
            if(!config.isActive()) {
                if(startCondition.await(3000, TimeUnit.MILLISECONDS)) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }

        return true;
    }

    @Override
    public boolean stopDatasource(String namespace) {

        if(StringUtils.isBlank(namespace)) {
            return false;
        }

        BinaryLogClient client = binLogClientFactory.getBinaryLogClient(namespace);
        if(client == null) {
            return false;
        }
        try {
            client.disconnect();
            logger.info("[" + namespace + "] 关闭datasource监听成功");
        } catch (IOException e) {
            logger.error("[" + namespace + "] 关闭datasource监听异常，{}", e);
            return false;
        }

        return true;
    }

}
