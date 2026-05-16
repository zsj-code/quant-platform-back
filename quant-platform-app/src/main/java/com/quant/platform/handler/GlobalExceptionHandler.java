package com.quant.platform.handler;

import com.quant.platform.common.api.Result;
import com.quant.platform.common.api.ResultCode;
import com.quant.platform.common.exception.BizException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public Result<Void> handleBadRequest(Exception e) {
        return Result.fail(ResultCode.BAD_REQUEST, extractMessage(e));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAny(Exception e) {
        log.error("Unhandled exception", e);
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }

    private static String extractMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            var fe = ex.getBindingResult().getFieldError();
            return fe == null ? ResultCode.BAD_REQUEST.defaultMessage() : fe.getField() + " " + fe.getDefaultMessage();
        }
        if (e instanceof BindException) {
            BindException ex = (BindException) e;
            var fe = ex.getBindingResult().getFieldError();
            return fe == null ? ResultCode.BAD_REQUEST.defaultMessage() : fe.getField() + " " + fe.getDefaultMessage();
        }
        if (e instanceof ConstraintViolationException) {
            ConstraintViolationException ex = (ConstraintViolationException) e;
            var cv = ex.getConstraintViolations().stream().findFirst().orElse(null);
            return cv == null ? ResultCode.BAD_REQUEST.defaultMessage() : cv.getMessage();
        }
        return e.getMessage() == null ? ResultCode.BAD_REQUEST.defaultMessage() : e.getMessage();
    }
}
