package cn.keking.project.binlogdistributor.app;

import cn.keking.project.binlogdistributor.app.util.leaderselector.LeaderSelector;
import cn.keking.project.binlogdistributor.app.util.leaderselector.LeaderSelectorException;
import cn.keking.project.binlogdistributor.app.util.leaderselector.LeaderSelectorListener;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author wanglaomo
 * @since 2019/7/30
 **/
public class LeaderSelectorTest {

    private Client client;
    private Lock lock;
    private Lease lease;
    //单位：秒
    private long leaseTTL = 10L;
    private String leaderPath = "/root/lock";
    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2);

    @Before
    public void setUp() {
        client = Client.builder().endpoints("http://localhost:2379").build();
        lock = client.getLockClient();
        lease = client.getLeaseClient();
    }

    @Test
    public void test1() throws IOException {

        LeaderSelector leaderSelector = new LeaderSelector(client, leaderPath, leaseTTL, "test1", new LeaderSelectorListener() {
            @Override
            public void afterTakeLeadership() throws InterruptedException {
                while (true) {

                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("检测到取消");
                        break;
                    }

                    System.out.println("test1开始工作了");
                    TimeUnit.SECONDS.sleep(1);
                }
            }

            @Override
            public boolean afterLosingLeadership() {
                System.out.println("test1失去领导权");
                return true;
            }
        } );

        try {
            leaderSelector.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.in.read();

    }

    @Test
    public void test2() throws IOException {

        LeaderSelector leaderSelector = new LeaderSelector(client, leaderPath, leaseTTL, "test2", new LeaderSelectorListener() {
            @Override
            public void afterTakeLeadership() throws InterruptedException {
                int limit = new Random().nextInt(10) + 5;
                int count = 0;
                while (true) {

                    System.out.println("test2开始工作了");
                    TimeUnit.SECONDS.sleep(1);
                }
            }

            @Override
            public boolean afterLosingLeadership() {
                System.out.println("test2失去领导权");
                return true;
            }
        } );

        try {
            leaderSelector.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.in.read();
    }

    @Test
    public void test3() throws IOException, LeaderSelectorException {
        LeaderSelector leaderSelector = new LeaderSelector(client, leaderPath, leaseTTL, "test3", new LeaderSelectorListener() {
            @Override
            public void afterTakeLeadership() throws InterruptedException {
                int limit = new Random().nextInt(10) + 5;
                int count = 0;
                while (true) {

                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("检测到取消");
                        break;
                    }

                    System.out.println("test3开始工作了");
                    TimeUnit.SECONDS.sleep(1);
                }
            }

            @Override
            public boolean afterLosingLeadership() {
                System.out.println("test3失去领导权");
                return true;
            }
        } );

        leaderSelector.start();
        System.in.read();
    }
}
