package com.uno.common.result;

import lombok.Getter;

/**
 * 统一状态码枚举
 */
@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "操作成功"),
    FAIL(500, "系统内部异常"),
    
    // 自定义业务错误
    PARAM_ERROR(400, "参数格式错误"),
    UNAUTHORIZED(401, "认证失败，请重新登录"),
    FORBIDDEN(403, "权限不足，拒绝访问"),
    NOT_FOUND(404, "请求资源不存在");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
