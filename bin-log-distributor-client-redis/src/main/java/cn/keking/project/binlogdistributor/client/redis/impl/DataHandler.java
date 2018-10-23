package cn.keking.project.binlogdistributor.client.redis.impl;

import cn.keking.project.binlogdistributor.client.BinLogDistributorClient;
import cn.keking.project.binlogdistributor.param.enums.LockLevel;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseDTO;
import cn.keking.project.binlogdistributor.param.model.dto.EventBaseErrDTO;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.JsonJacksonMapCodec;

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
    public transient static Set<String> DATA_KEY_IN_PROCESS = new ConcurrentHashMap<String, String>().keySet("");

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
        if (dataKey.contains(noneLock)) {
            doRunWithoutLock();
        } else {
            doRunWithLock();
        }
    }

    private void doRunWithoutLock() {
        try {
            RQueue<EventBaseDTO> queue = redissonClient.getQueue(dataKey);
            EventBaseDTO dto;
            while ((dto = queue.poll()) != null) {
                doHandleWithoutLock(dto, retryTimes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("接收处理数据失败：" + e.toString());
        }

    }

    private void doRunWithLock() {
        RLock rLock = redissonClient.getLock(dataKeyLock);
        EventBaseDTO dto;
        boolean lockRes = false;
        try {
            // 尝试加锁，最多等待50ms(防止过多线程等待)，上锁以后6个小时自动解锁(防止redis队列太长，当前拿到锁的线程处理时间过长)
            lockRes = rLock.tryLock(50, 6 * 3600 * 1000, TimeUnit.MILLISECONDS);
            if (!lockRes) {
                return;
            }
            DATA_KEY_IN_PROCESS.add(dataKey);
            //拿到锁之后再获取队列
            RQueue<EventBaseDTO> queue = redissonClient.getQueue(dataKey);
            if (!queue.isExists() || queue.isEmpty()) {
                return;
            }
            //拿到锁且 队列不为空 进入
            while ((dto = queue.peek()) != null) {
                //处理完毕，把数据从队列摘除
                boolean handleRes = doHandleWithLock(dto, 0);
                if (handleRes) {
                    queue.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.severe("接收处理数据失败：" + e.toString());
        } finally {
            //forceUnlock是可以释放别的线程拿到的锁的，需要判断是否是当前线程持有的锁
            if (lockRes) {
                rLock.forceUnlock();
                rLock.delete();
                DATA_KEY_IN_PROCESS.remove(dataKey);
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
                RMap<String, EventBaseErrDTO> errMap = redissonClient.getMap(errMapKey, new JsonJacksonMapCodec(String.class, EventBaseErrDTO.class));
                errMap.put(dto.getUuid(), new EventBaseErrDTO(dto, e, dataKey));
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
                RMap<String, EventBaseErrDTO> errMap = redissonClient.getMap(errMapKey, new JsonJacksonMapCodec(String.class, EventBaseErrDTO.class));
                errMap.remove(dto.getUuid());
            }
            return true;
        } catch (Exception e) {
            if (retryTimes.intValue() >= 5) {
                return true;
            }
            log.severe(e.toString());
            e.printStackTrace();
            log.log(Level.SEVERE, "第" + ++retryTimes + "次重试");
            RMap<String, EventBaseErrDTO> errMap = redissonClient.getMap(errMapKey, new JsonJacksonMapCodec(String.class, EventBaseErrDTO.class));
            errMap.put(dto.getUuid(), new EventBaseErrDTO(dto, e, dataKey));
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e1) {
                log.severe(e1.toString());
                e1.printStackTrace();
            }
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
