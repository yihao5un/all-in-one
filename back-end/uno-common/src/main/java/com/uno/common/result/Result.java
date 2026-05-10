package com.uno.common.result;

import lombok.Data;

/**
 * 全局统一泛型返回类
 */
@Data
public class Result<T> {

    // 状态码
    private Integer code;
    // 提示信息
    private String message;
    // 响应数据
    private T data;
    
    protected Result() {}

    // 构建 Result
    public static <T> Result<T> build(T data, ResultCodeEnum resultCodeEnum) {
        Result<T> result = new Result<>();
        if (data != null) {
            result.setData(data);
        }
        result.setCode(resultCodeEnum.getCode());
        result.setMessage(resultCodeEnum.getMessage());
        return result;
    }

    // 操作成功带数据
    public static <T> Result<T> success(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }
    
    // 操作成功不带数据
    public static <T> Result<T> success() {
        return success(null);
    }

    // 操作失败带数据
    public static <T> Result<T> fail(T data) {
        return build(data, ResultCodeEnum.FAIL);
    }
    
    // 操作失败不带数据
    public static <T> Result<T> fail() {
        return fail(null);
    }
    
    // 自定义提示信息链式调用
    public Result<T> message(String msg){
        this.setMessage(msg);
        return this;
    }

    // 自定义状态码链式调用
    public Result<T> code(Integer code){
        this.setCode(code);
        return this;
    }
}
