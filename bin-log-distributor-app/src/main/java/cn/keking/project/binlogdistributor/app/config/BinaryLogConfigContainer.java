package cn.keking.project.binlogdistributor.app.config;

import cn.keking.project.binlogdistributor.param.enums.Constants;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author wanglaomo
 * @since 2019/6/4
 **/
@ConfigurationProperties(prefix = "binaryLog")
@Configuration
public class BinaryLogConfigContainer {

    private static final String CONFIG_KEY = "DATASOURCE-CONFIG";

    @Autowired
    private RedissonClient redissonClient;

    private List<BinaryLogConfig> configs;

    public synchronized List<BinaryLogConfig> initAllConfigs() {

        if(configs == null) {
            configs = new ArrayList<>();
        }

        List<BinaryLogConfig> persistentConfigs = getPersistentConfigs();
        configs.addAll(persistentConfigs);

        if(configs.isEmpty()) {
            throw new IllegalArgumentException("There is no available BinaryLog config, please check your configuration!");
        }
        Set<String> namespaces = new HashSet<>();
        configs.stream().forEach(config -> {

            if(StringUtils.isEmpty(config.getNamespace())) {
                throw new IllegalArgumentException("You need to config namespace!");
            }
            if(!namespaces.add(config.getNamespace())) {
                throw new IllegalArgumentException("Duplicated namespace!");
            }
        });
        namespaces.clear();
        return configs;
    }

    public List<BinaryLogConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(List<BinaryLogConfig> configs) {
        this.configs = configs;
    }

    public List<BinaryLogConfig> getPersistentConfigs() {

        return redissonClient.getList(Constants.REDIS_PREFIX + CONFIG_KEY);
    }

    public synchronized boolean persistConfig(BinaryLogConfig newConfig) {

        boolean exist = configs.stream().anyMatch(c -> c.getNamespace().equals(newConfig.getNamespace()));

        if(exist) {
            return false;
        }

        List<BinaryLogConfig> persistentConfigs = getPersistentConfigs();
        persistentConfigs.add(newConfig);
        configs.add(newConfig);

        return true;
    }

    public synchronized boolean removeConfig(String namespace) {

        if(StringUtils.isEmpty(namespace)) {
            return false;
        }


        Iterator<BinaryLogConfig> iterator = configs.iterator();
        while (iterator.hasNext()){
            BinaryLogConfig config = iterator.next();
            if(config.getNamespace().equals(namespace)) {
                iterator.remove();
                break;
            }
        }

        List<BinaryLogConfig> persistentConfigs = getPersistentConfigs();
        iterator = persistentConfigs.iterator();
        while (iterator.hasNext()){
            BinaryLogConfig config = iterator.next();
            if(config.getNamespace().equals(namespace)) {
                iterator.remove();
                break;
            }
        }

        return true;
    }

    public synchronized BinaryLogConfig getConfigByNamespace(String namespace) {

        Optional<BinaryLogConfig> optional = configs.stream().filter(config -> namespace.equals(config.getNamespace())).findAny();

        if (optional.isPresent()) {
            return optional.get();
        }

        return null;
    }
}
