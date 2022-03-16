package com.actionworks.flashsale.lock;

/**
 * todo: 分布式锁的接口，便于与具体分布式锁实现解耦
 */
public interface DistributedLockFactoryService {
    DistributedLock getDistributedLock(String key);
}
