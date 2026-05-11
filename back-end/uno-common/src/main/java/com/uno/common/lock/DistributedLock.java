package com.uno.common.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的名称 (支持 SpEL 表达式)
     */
    String key();

    /**
     * 锁的前缀
     */
    String prefix() default "uno:lock:";

    /**
     * 等待锁超时时间，默认 30 秒
     */
    long waitTime() default 30;

    /**
     * 自动释放锁时间，默认 60 秒
     * 注意：Redisson 有看门狗机制，如果 leaseTime 为 -1，则会触发看门狗
     */
    long leaseTime() default 60;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
