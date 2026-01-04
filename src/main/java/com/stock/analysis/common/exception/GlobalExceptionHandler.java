package com.stock.analysis.common.exception;

import com.stock.analysis.common.result.ErrorCode;
import com.stock.analysis.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("Business Exception: code={}, msg={}", e.getCode(), e.getMsg());
        return Result.error(e.getCode(), e.getMsg());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception e) {
        log.error("Validation Exception: {}", e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("System Exception: ", e);
        return Result.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
