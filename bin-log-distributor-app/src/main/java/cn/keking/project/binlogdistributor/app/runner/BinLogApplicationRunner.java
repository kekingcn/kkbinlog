package cn.keking.project.binlogdistributor.app.runner;

import cn.keking.project.binlogdistributor.app.config.BinaryLogConfigContainer;
import cn.keking.project.binlogdistributor.app.service.DistributorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author T-lih
 * @modified by
 */
@Component
public class BinLogApplicationRunner implements ApplicationRunner {

    private final ApplicationContext context;

    private final BinaryLogConfigContainer binaryLogConfigContainer;

    public BinLogApplicationRunner(ApplicationContext context, BinaryLogConfigContainer binaryLogConfigContainer) {
        this.context = context;
        this.binaryLogConfigContainer = binaryLogConfigContainer;
    }

    @Override
    public void run(ApplicationArguments applicationArguments) {
        Map<String, DistributorService> distributorServiceMap = context.getBeansOfType(DistributorService.class);
        distributorServiceMap.forEach((s, distributorService) -> {
            distributorService.startDistribute();
        });
        binaryLogConfigContainer.registerConfigCommandWatcher();
    }
}
