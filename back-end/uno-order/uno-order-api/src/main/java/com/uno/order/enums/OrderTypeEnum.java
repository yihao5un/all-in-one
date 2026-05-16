package com.uno.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单类型枚举
 */
@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    ONBOARD("ONBOARD", "入职调派");

    private final String code;
    private final String desc;
}
