package com.uno.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 第三方系统同步状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderSyncStatusEnum {

    NOT_SYNCED("NOT_SYNCED", "未同步"),
    SYNCING("SYNCING", "同步中"),
    SUCCESS("SUCCESS", "同步成功"),
    FAILED("FAILED", "同步失败");

    private final String code;
    private final String desc;
}
