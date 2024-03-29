package com.mr.web.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
 
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
 
/**
* 类描述：Curator实现的分布式锁
* 创建人：simonsfan
*/
public class DistributedLock {
 
    private static CuratorFramework curatorFramework;
 
    private static InterProcessMutex interProcessMutex;
 
    private static final String connectString = "localhost:2181";
 
    private static final String root = "/root";
 
    private static ExecutorService executorService;
 
    private String lockName;
 
    public String getLockName() {
        return lockName;
    }
 
    public void setLockName(String lockName) {
        this.lockName = lockName;
    }
 
    static {
        curatorFramework = CuratorFrameworkFactory.builder().connectString(connectString).connectionTimeoutMs(5000).sessionTimeoutMs(5000).retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
        executorService = Executors.newCachedThreadPool();
        curatorFramework.start();
    }
 
    public DistributedLock(String lockName) {
        this.lockName = lockName;
        interProcessMutex = new InterProcessMutex(curatorFramework, root.concat(lockName));
    }
 
    /*上锁*/
    public boolean tryLock() {
        int count = 0;
        try {
            while (!interProcessMutex.acquire(100, TimeUnit.SECONDS)) {
                count++;
                if (count > 3) {
                    TimeUnit.SECONDS.sleep(1);
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
 
    /*释放*/
    public void releaseLock() {
        try {
            if (interProcessMutex != null) {
                interProcessMutex.release();
            }
            curatorFramework.delete().inBackground(new BackgroundCallback() {
                @Override
                public void processResult(CuratorFramework curatorFramework, CuratorEvent curatorEvent) throws Exception {
 
                }
            }, executorService).forPath(root.concat(lockName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
}