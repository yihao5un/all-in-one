package com.uno.common.exception;

import com.uno.common.result.Result;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

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
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> error(Exception e) {
        log.error("【系统全局异常拦截】: ", e);
        
        // 核心优化：递归拆包 (处理 Seata/AOP 等多层包装的异常)
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
            if (t instanceof UnoException) {
                return error((UnoException) t);
            }
            if (t instanceof feign.FeignException) {
                return error((feign.FeignException) t);
            }
        }
        
        return Result.<String>fail().message("系统开小差了，请稍后再试");
    }

    /**
     * 业务自定义异常处理 (UnoException)
     */
    @ExceptionHandler(UnoException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> error(UnoException e) {
        log.error("【业务异常拦截】 - 错误码: {}, 错误信息: {}", e.getCode(), e.getMessage());
        return Result.<String>fail().code(e.getCode()).message(e.getMessage());
    }

    /**
     * Feign 远程调用异常处理
     */
    @ExceptionHandler(FeignException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> error(FeignException e) {
        log.error("【远程调用异常拦截】: ", e);
        return Result.<String>fail().message("远程服务调用失败: " + e.getMessage());
    }
}
