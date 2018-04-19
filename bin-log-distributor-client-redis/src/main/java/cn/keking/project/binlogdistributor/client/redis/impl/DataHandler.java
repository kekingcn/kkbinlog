package cn.keking.project.binlogdistributor.client.redis.impl;

import cn.keking.project.binlogdistributor.client.BinLogDistributorClient;
import cn.keking.project.binlogdistributor.param.enums.LockLevel;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseErrDTO;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author zhenhui
 * @Ddate Created in 2018/2018/4/4/4:35 PM
 * @modified by
 */
public class DataHandler implements Runnable {
    private final static Logger log = Logger.getLogger(DataHandler.class.toString());

    /**
     * 识别本线程，暂时未使用
     */
    private String uuid = UUID.randomUUID().toString();
    private static String noneLock = LockLevel.NONE.name();
    private String dataKey;
    private String clientId;
    private BinLogDistributorClient binLogDistributorClient;
    private RedissonClient redissonClient;
    private String errMapKey;
    private String dataKeyLock;
    /**
     * 重试次数
     */
    private int retryTimes = 3;
    /**
     * 重试间隔,单位毫秒
     */
    private long retryInterval = 10 * 1000;
    /**
     * 正在处理的队列
     */
    public static Set<String> DATA_KEY_IN_PROCESS = new ConcurrentHashMap<String, String>().keySet("");

    public DataHandler(String dataKey, String clientId, BinLogDistributorClient binLogDistributorClient, RedissonClient redissonClient) {
        this.dataKey = dataKey;
        this.clientId = clientId;
        this.binLogDistributorClient = binLogDistributorClient;
        this.redissonClient = redissonClient;
        this.errMapKey = "BIN-LOG-ERR-MAP-".concat(clientId);
        this.dataKeyLock = dataKey.concat("-Lock");
    }

    @Override
    public void run() {
        try {
            if (dataKey.contains(noneLock)) {
                doRunWithoutLock();
            } else {
                RLock lock = redissonClient.getLock(dataKeyLock);
                if (lock.tryLock(0, 3 * retryInterval, TimeUnit.MILLISECONDS)) {
                    DATA_KEY_IN_PROCESS.add(dataKey);
                    doRunWithLock(lock);
                    lock.unlock();
                    DATA_KEY_IN_PROCESS.remove(dataKey);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("接收处理数据失败：" + e.toString());
        }
    }

    private void doRunWithoutLock() {
        RQueue<EventBaseDTO> queue = redissonClient.getQueue(dataKey);
        EventBaseDTO dto;
        while ((dto = queue.poll()) != null) {
            doHandleWithoutLock(dto, retryTimes);
        }
    }

    private void doRunWithLock(RLock lock) throws InterruptedException {
        RQueue<EventBaseDTO> queue = redissonClient.getQueue(dataKey);
        EventBaseDTO dto;
        while (lock.tryLock(0, 3 * retryInterval, TimeUnit.MILLISECONDS) && (dto = queue.peek()) != null) {
            if (doHandleWithLock(dto, 0) && lock.isHeldByCurrentThread()) {
                queue.poll();
            }
        }
    }

    /**
     * handle及处理异常，出现异常未成功处理返回false，处理成功返回true
     *
     * @param dto
     * @param leftRetryTimes
     */
    private void doHandleWithoutLock(EventBaseDTO dto, int leftRetryTimes) {
        try {
            binLogDistributorClient.handle(dto);
        } catch (Exception e) {
            log.severe(e.toString());
            e.printStackTrace();
            if (leftRetryTimes == 1) {
                RMap<String, EventBaseErrDTO> errMap = redissonClient.getMap(errMapKey);
                errMap.put(dto.getUuid(), new EventBaseErrDTO(dto, e,dataKey));
            } else {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e1) {
                    log.severe(e1.toString());
                    e1.printStackTrace();
                }
                log.log(Level.SEVERE, "还剩{}次重试", --leftRetryTimes);
                doHandleWithoutLock(dto, leftRetryTimes);
            }
        }
    }

    /**
     * handle及处理异常，出现异常未成功处理返回false，处理成功返回true
     *
     * @param dto
     * @param retryTimes
     */
    private boolean doHandleWithLock(final EventBaseDTO dto, Integer retryTimes) {
        try {
            binLogDistributorClient.handle(dto);
            //如果之前有异常，恢复正常，那就处理
            if (retryTimes != 0) {
                RMap<String, EventBaseErrDTO> errMap = redissonClient.getMap(errMapKey);
                errMap.remove(dto.getUuid());
            }
            return true;
        } catch (Exception e) {
            log.severe(e.toString());
            e.printStackTrace();
            RMap<String, EventBaseErrDTO> errMap = redissonClient.getMap(errMapKey);
            errMap.put(dto.getUuid(), new EventBaseErrDTO(dto, e,dataKey));
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e1) {
                log.severe(e1.toString());
                e1.printStackTrace();
            }
            log.log(Level.SEVERE, "第{}次重试", ++retryTimes);
            return doHandleWithLock(dto, retryTimes);
        }
    }


    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getUuid() {
        return uuid;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public BinLogDistributorClient getBinLogDistributorClient() {
        return binLogDistributorClient;
    }

    public void setBinLogDistributorClient(BinLogDistributorClient binLogDistributorClient) {
        this.binLogDistributorClient = binLogDistributorClient;
    }
}
