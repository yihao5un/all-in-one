package com.uno.common.util;

import com.uno.common.enums.BizNoPrefixEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 全局通用的分布式业务单号生成器
 * 规则：前缀 + 日期(yyyyMMdd) + 自增流水号
 */
@Component
@RequiredArgsConstructor
public class BizNoGenerator {

    private final StringRedisTemplate redisTemplate;

    private static final String SEQ_KEY_PREFIX = "seq:";

    /**
     * 生成通用业务单号
     * 
     * @param prefix 业务前缀 (如 ONB:入职, SET:结算, INV:发票)
     * @param seqLength 流水号位长度 (如 3位支持单日999单, 5位支持单日99999单)
     * @return 业务单号
     */
    public String generate(String prefix, int seqLength) {
        // 1. 获取当前日期字符串
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 2. 构造 Redis Key
        String key = SEQ_KEY_PREFIX + prefix + ":" + dateStr;
        
        // 3. 原子递增
        Long sequence = redisTemplate.opsForValue().increment(key);
        
        // 4. 首单设置过期时间
        if (sequence != null && sequence == 1) {
            redisTemplate.expire(key, 48, TimeUnit.HOURS);
        }
        
        // 5. 格式化补齐位数
        String format = "%0" + seqLength + "d";
        String seqStr = String.format(format, sequence);
        
        return prefix + dateStr + seqStr;
    }

    /**
     * 默认生成器 (3位流水号)
     */
    public String generate(String prefix) {
        return generate(prefix, 3);
    }

    /**
     * 基于枚举生成单号 (推荐)
     */
    public String generate(BizNoPrefixEnum prefixEnum) {
        return generate(prefixEnum.getPrefix(), 3);
    }

    /**
     * 基于枚举生成单号 (支持自定义位长度)
     */
    public String generate(BizNoPrefixEnum prefixEnum, int seqLength) {
        return generate(prefixEnum.getPrefix(), seqLength);
    }
}
