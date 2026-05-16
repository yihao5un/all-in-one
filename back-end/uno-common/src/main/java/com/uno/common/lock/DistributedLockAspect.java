package com.uno.common.lock;

import com.uno.common.exception.UnoException;
import com.uno.common.result.ResultCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 分布式锁切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 确保在事务和 Seata 之前运行
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock) || @within(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 如果 distributedLock 为空 (比如从类注解匹配到的)，则尝试从方法上获取
        if (distributedLock == null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            distributedLock = signature.getMethod().getAnnotation(DistributedLock.class);
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 解析 SpEL 表达式获取动态 Key
        String lockKey;
        try {
            lockKey = parseSpel(distributedLock.key(), method, args);
        } catch (Exception e) {
            log.error("❌ [分布式锁] SpEL 解析失败: key={}, error={}", distributedLock.key(), e.getMessage());
            lockKey = "default";
        }
        String fullKey = distributedLock.prefix() + lockKey;

        log.info("🔐 [分布式锁] 准备进入拦截, Key: {}", fullKey);
        RLock lock = redissonClient.getLock(fullKey);
        
        log.info("🔐 [分布式锁] 尝试获取锁: {}", fullKey);
        boolean isLocked = false;
        try {
            // 尝试加锁
            isLocked = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.unit());
            if (isLocked) {
                log.info("🔓 [分布式锁] 成功获取锁: {}", fullKey);
                return joinPoint.proceed();
            } else {
                log.warn("⏳ [分布式锁] 获取锁失败 (超时): {}", fullKey);
                throw new UnoException("系统繁忙，请稍后再试", ResultCodeEnum.FAIL.getCode());
            }
        } catch (InterruptedException e) {
            log.error("❌ [分布式锁] 获取锁异常", e);
            Thread.currentThread().interrupt();
            throw new UnoException("系统异常", ResultCodeEnum.FAIL.getCode());
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔑 [分布式锁] 已释放锁: {}", fullKey);
            }
        }
    }

    /**
     * 解析 SpEL 表达式
     */
    private String parseSpel(String spel, Method method, Object[] args) {
        String[] params = nameDiscoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                context.setVariable(params[i], args[i]);
            }
        }
        Expression expression = parser.parseExpression(spel);
        Object value = expression.getValue(context);
        return value != null ? value.toString() : "";
    }
}
