package cn.keking.project.binlogdistributor.app.service;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfig;
import cn.keking.project.binlogdistributor.app.model.vo.BinaryLogConfigVO;

import java.util.List;

/**
 * @author wanglaomo
 * @since 2019/6/10
 **/
public interface DistributorService {

    void startDistribute();

    boolean persistDatasourceConfig(BinaryLogConfig config);

    boolean removeDatasourceConfig(String namespace);

    List<BinaryLogConfigVO> getAllConfigs();

    boolean startDatasource(String namespace);

    boolean stopDatasource(String namespace);
}
