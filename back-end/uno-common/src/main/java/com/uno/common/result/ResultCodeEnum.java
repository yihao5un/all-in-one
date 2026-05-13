package com.uno.common.result;

import lombok.Getter;

/**
 * 统一响应码枚举
 * 200: 成功
 * 2xx: 业务级警告/预期内错误 (如登录失败、参数校验失败)
 * 4xx: 客户端请求权限/资源错误
 * 5xx: 系统底层/未知错误
 */
@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "成功"),
    FAIL(500, "系统繁忙，请稍后再试"),
    
    // --- 2xx: 业务相关错误 ---
    USER_NOT_FOUND(201, "账号不存在"),
    PASSWORD_ERROR(202, "密码错误"),
    ACCOUNT_DISABLED(203, "账号已被禁用"),
    PARAM_ERROR(204, "参数校验失败"),
    
    // --- 4xx: 权限相关错误 ---
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "权限不足，拒绝访问"),
    
    // --- 5xx: 服务/链路相关错误 ---
    SERVICE_ERROR(500, "服务异常"),
    REMOTE_CALL_ERROR(501, "远程服务调用失败"),
    IDEMPOTENT_REJECT(502, "请勿重复提交");

    private Integer code;
    private String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
