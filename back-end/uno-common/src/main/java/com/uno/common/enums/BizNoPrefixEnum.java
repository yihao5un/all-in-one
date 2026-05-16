package com.uno.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务单号前缀枚举
 */
@Getter
@AllArgsConstructor
public enum BizNoPrefixEnum {

    /**
     * 入职申请订单
     */
    ONBOARD("ONB", "入职订单"),

    /**
     * 结算/账单
     */
    SETTLEMENT("SET", "结算单"),

    /**
     * 退款单
     */
    REFUND("REF", "退款单"),

    /**
     * 产品订单
     */
    PRODUCT("PRD", "产品订单");

    private final String prefix;
    private final String desc;
}
