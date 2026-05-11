package com.uno.common.idempotent;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等性注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等 Key 的前缀
     */
    String prefix() default "uno:idempotent:";

    /**
     * 幂等 Key (支持 SpEL 表达式)
     * 例如：#dto.orderNo
     */
    String key();

    /**
     * 有效期 (默认 24 小时)
     * 在此期间，相同的 Key 将被视为重复提交
     */
    long expire() default 24;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.HOURS;

    /**
     * 提示消息
     */
    String message() default "请勿重复提交";
}
