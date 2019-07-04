package cn.keking.project.binlogdistributor.app.runner;

import cn.keking.project.binlogdistributor.app.service.DistributorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author T-lih
 * @modified by
 */
@Component
public class BinLogApplicationRunner implements ApplicationRunner {

    @Autowired
    private DistributorService distributorService;

    @Override
    public void run(ApplicationArguments applicationArguments) {

        distributorService.startDistribute();
    }
}
