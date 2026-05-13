package com.uno.common.handler;

import com.uno.common.exception.UnoException;
import com.uno.common.result.Result;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 增强版全局异常处理器
 * 整合了业务异常、远程调用异常以及多层异常拆包逻辑
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理系统级自定义业务异常 (UnoException)
     */
    @ExceptionHandler(UnoException.class)
    public Result<Object> handleUnoException(UnoException e) {
        log.error("【系统异常】业务错误: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail()
                .code(e.getCode())
                .message(e.getMessage());
    }

    /**
     * 处理 Feign 远程调用异常
     */
    @ExceptionHandler(FeignException.class)
    public Result<Object> handleFeignException(FeignException e) {
        log.error("【远程调用异常拦截】: ", e);
        return Result.fail().message("远程服务调用失败: " + e.getMessage());
    }

    /**
     * 处理其他未知异常 (Exception)
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        log.error("【系统异常】未知错误: ", e);
        
        // 核心优化：递归拆包 (处理 Seata/AOP 等多层包装的异常)
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
            if (t instanceof UnoException) {
                return handleUnoException((UnoException) t);
            }
            if (t instanceof FeignException) {
                return handleFeignException((FeignException) t);
            }
        }
        
        return Result.fail()
                .message("系统繁忙，请稍后再试: " + e.getMessage());
    }
}
