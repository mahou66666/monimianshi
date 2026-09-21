package com.a05.admin.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.StringJoiner;

/**
 * Global exception handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        StringJoiner joiner = new StringJoiner("; ");
        bindingResult.getFieldErrors().forEach(fe ->
                joiner.add(fe.getField() + ": " + fe.getDefaultMessage())
        );
        log.warn("[参数校验失败] {}", joiner);
        return Result.error(ResultCode.VALIDATION_FAILED.getCode(), joiner.toString());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("[上传文件超过限制] {}", ex.getMessage());
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "PDF 文件过大，单个文件不能超过 20MB");
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        log.error("[业务异常] {}", ex.getMessage(), ex);
        return Result.error(ResultCode.ERROR.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("[系统异常] {}", ex.getMessage(), ex);
        return Result.error(ResultCode.ERROR);
    }
}
