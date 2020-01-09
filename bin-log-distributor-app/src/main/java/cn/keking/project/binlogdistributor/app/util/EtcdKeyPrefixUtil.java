package cn.keking.project.binlogdistributor.app.util;

import cn.keking.project.binlogdistributor.param.enums.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wanglaomo
 * @since 2019/10/16
 **/
@Component
public class EtcdKeyPrefixUtil {

    @Value("${spring.etcd.root:/root/}")
    private String root;

    private AtomicReference<String> prefixCache = new AtomicReference<>();

    public String getPrefix() {

        String prefix = prefixCache.get();
        if(!StringUtils.isEmpty(prefix)) {
            return prefix;
        }

        // init
        if(!root.startsWith(Constants.PATH_SEPARATOR)) {
            root = Constants.PATH_SEPARATOR.concat(root);
        }

        if(!root.endsWith(Constants.PATH_SEPARATOR)) {
            root = root.concat(Constants.PATH_SEPARATOR);
        }

        prefix = root.concat(Constants.DEFAULT_ETCD_METADATA_PREFIX);
        prefixCache.compareAndSet(null, prefix);
        prefix = prefixCache.get();

        return prefix;
    }

    public String withPrefix(String key) {

        if(StringUtils.isEmpty(key)) {
            return getPrefix();
        }

        return getPrefix().concat(key);
    }
}
