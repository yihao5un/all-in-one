package com.uno.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单主状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    CREATED("CREATED", "已创建"),
    PROCESSING("PROCESSING", "扣减名额中"),
    WAIT_EXTERNAL_SYNC("WAIT_EXTERNAL_SYNC", "等待三方同步"),
    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),
    SETTLED("SETTLED", "已结算"),
    CLOSED("CLOSED", "已关闭"),
    SYNC_FAILED("SYNC_FAILED", "三方同步失败");

    private final String code;
    private final String desc;
}
