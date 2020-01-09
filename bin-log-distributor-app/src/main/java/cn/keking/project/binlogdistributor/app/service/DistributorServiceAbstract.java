package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.model.BinLogCommand;
import cn.keking.project.binlogdistributor.app.model.ServiceStatus;
import cn.keking.project.binlogdistributor.app.model.enums.BinLogCommandType;
import cn.keking.project.binlogdistributor.app.model.vo.BinaryLogConfigVO;
import cn.keking.project.binlogdistributor.pub.DataPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * @author: kl @kailing.pub
 * @date: 2020/1/7
 */
public abstract class DistributorServiceAbstract implements DistributorService {

    protected static final Logger logger = LoggerFactory.getLogger(DistributorServiceAbstract.class);

    protected ScheduledExecutorService scheduledExecutorService;

    @Autowired
    protected EtcdService etcdService;

    @Autowired
    protected BinaryLogConfigContainer binaryLogConfigContainer;

    @Override
    public List<BinaryLogConfigVO> getAllConfigs() {

        return binaryLogConfigContainer.getConfigs().stream().map(BinaryLogConfigVO::new).collect(Collectors.toList());
    }

    @Override
    public boolean persistDatasourceConfig(BinaryLogConfig config) {

        boolean res = binaryLogConfigContainer.addConfig(config);
        if(!res) {
            return false;
        }
        return true;
    }

    @Override
    public boolean removeDatasourceConfig(String namespace) {

        BinaryLogConfig removedConfig = binaryLogConfigContainer.removeConfig(namespace);
        if(removedConfig == null) {
            return false;
        }
        return true;
    }

    /**
     * 向etcd发送开启数据源命令
     *
     *
     * @param namespace
     * @param delegatedIp
     * @return
     */
    @Override
    public boolean startDatasource(String namespace, String delegatedIp) {

        BinLogCommand binLogCommand = new BinLogCommand(namespace, delegatedIp, BinLogCommandType.START_DATASOURCE);
        return etcdService.sendBinLogCommand(binLogCommand);
    }

    /**
     * 向etcd发送关闭数据源命令
     *
     * @param namespace
     * @return
     */
    @Override
    public boolean stopDatasource(String namespace) {

        BinLogCommand binLogCommand = new BinLogCommand(namespace, BinLogCommandType.STOP_DATASOURCE);
        return etcdService.sendBinLogCommand(binLogCommand);
    }

    /**
     * 获得能够执行任务的主机ip
     * @return
     */
    @Override
    public List<ServiceStatus> getServiceStatus() {
        return etcdService.getServiceStatus();
    }

}
