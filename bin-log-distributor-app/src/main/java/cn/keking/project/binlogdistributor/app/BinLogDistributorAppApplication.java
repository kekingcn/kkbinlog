package cn.keking.project.binlogdistributor.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;


/**
 * @author T-lih
 */
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class BinLogDistributorAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(BinLogDistributorAppApplication.class, args);
    }
}
