package com.uno.common.exception;

import com.uno.common.result.ResultCodeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统级自定义业务异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnoException extends RuntimeException {

    private Integer code;

    /**
     * 只接收消息，状态码默认为失败 (500 或 201)
     * @param message 异常信息
     */
    public UnoException(String message) {
        super(message);
        this.code = ResultCodeEnum.FAIL.getCode();
    }

    /**
     * 接收自定义状态码和消息
     * @param message 异常信息
     * @param code 状态码
     */
    public UnoException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    /**
     * 接收枚举类型构建异常
     * @param resultCodeEnum 异常枚举
     */
    public UnoException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }
}
