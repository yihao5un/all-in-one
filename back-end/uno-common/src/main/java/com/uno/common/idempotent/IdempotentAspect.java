package com.uno.common.idempotent;

import com.uno.common.exception.UnoException;
import com.uno.common.result.ResultCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 幂等性切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // 在分布式锁之后运行，即：先拿锁，再验幂等
public class IdempotentAspect {

    private final RedissonClient redissonClient;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 1. 解析 SpEL 获取幂等 Key
        String idempotentKey = parseSpel(idempotent.key(), method, args);
        String fullKey = idempotent.prefix() + idempotentKey;

        RBucket<String> bucket = redissonClient.getBucket(fullKey);

        // 2. 尝试占位 (SET NX)
        // 值为当前时间，仅作记录
        boolean success = bucket.trySet(LocalDateTime.now().toString(), idempotent.expire(), idempotent.unit());

        if (success) {
            log.info("📌 [幂等插件] Key 占位成功: {}", fullKey);
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                // 如果执行失败，删除 Key，允许重试 (根据业务需求，有的幂等失败也不允许重试)
                bucket.delete();
                log.warn("📌 [幂等插件] 业务执行失败，已释放 Key: {}", fullKey);
                throw e;
            }
        } else {
            log.warn("🚫 [幂等插件] 检测到重复提交, Key: {}", fullKey);
            throw new UnoException(idempotent.message(), ResultCodeEnum.FAIL.getCode());
        }
    }

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
