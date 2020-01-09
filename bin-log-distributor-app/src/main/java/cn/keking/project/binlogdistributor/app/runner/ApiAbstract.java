package cn.keking.project.binlogdistributor.app.runner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author wanglaomo
 * @since 2019/10/23
 **/
@Component
public class ApiAbstract {

    private static final Logger logger = LoggerFactory.getLogger(ApiAbstract.class);

    @Value("${service.name:middleground-binlog}")
    private String clientId;

    @Autowired
    private RedissonClient redissonClient;

    @PostConstruct
    public void afterPropertiesSet() {
        ClassPathResource classPathResource = new ClassPathResource("api.json");
        try (InputStreamReader reader = new InputStreamReader(classPathResource.getInputStream(),"utf-8")) {
            StringBuilder api = new StringBuilder();
            int tempchar;
            while ((tempchar = reader.read()) != -1) {
                api.append((char) tempchar);
            }
            String roleResource = api.toString();
            if(!StringUtils.isEmpty(roleResource)){
                RBucket<Object> bucket = redissonClient.getBucket(clientId.concat("_").concat("resource"));
                bucket.set(roleResource);
                logger.info("初始化or更新url资源完成:" + roleResource);
            }
        } catch (IOException e) {
            logger.error("Api抽取上传失败", e);
        }
    }
}
