package com.uno.common.exception;

import com.uno.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理，避免任何报错堆栈直接暴露给前端
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 顶级异常处理 (Exception)
     * 拦截所有未预知的异常，防止500报错外溢
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<String> error(Exception e) {
        log.error("【系统全局异常拦截】: ", e);
        return Result.<String>fail().message("系统开小差了，请稍后再试");
    }

    /**
     * 业务自定义异常处理 (UnoException)
     * 拦截业务代码主动抛出的已知异常，返回约定状态码
     */
    @ExceptionHandler(UnoException.class)
    @ResponseBody
    public Result<String> error(UnoException e) {
        log.error("【业务异常拦截】 - 错误码: {}, 错误信息: {}", e.getCode(), e.getMessage());
        return Result.<String>fail().code(e.getCode()).message(e.getMessage());
    }
}
