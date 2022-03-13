package com.actionworks.flashsale.controller.exception;

import com.actionworks.flashsale.app.exception.BizException;
import com.actionworks.flashsale.domain.exception.DomainException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.reflect.UndeclaredThrowableException;

import static com.actionworks.flashsale.controller.constants.ExceptionCode.*;

/**
 * todo: 使用 @ControllerAdvice 和 @ExceptionHandler 可以做到全局异常，但是无法做到统一异常，例如 post 请求的方法，前端使用成了 get，
 * 这类异常框架层面直接处理了，导致我们无法做统一的异常格式返回。
 * <p>
 * ResponseEntityExceptionHandler 类搭配 @ControllerAdvice 使用可以让我们定义统一的异常格式，
 *  包括之前框架处理的异常，我们可以改为自己进行处理。
 */
@ControllerAdvice
public class BadRequestExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(InternalExceptionHandler.class);

    @ExceptionHandler(value = {BizException.class, FlowException.class, AuthException.class, DomainException.class})
    protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        if (ex instanceof UndeclaredThrowableException) {
            if (((UndeclaredThrowableException) ex).getUndeclaredThrowable() instanceof FlowException) {
                exceptionResponse.setErrorCode(LIMIT_BLOCK.getCode());
                exceptionResponse.setErrorMessage(LIMIT_BLOCK.getDesc());
            }
        } else if (ex instanceof BizException || ex instanceof DomainException) {
            exceptionResponse.setErrorCode(BIZ_ERROR.getCode());
            exceptionResponse.setErrorMessage(ex.getMessage());
        } else if (ex instanceof AuthException) {
            exceptionResponse.setErrorCode(AUTH_ERROR.getCode());
            exceptionResponse.setErrorMessage(AUTH_ERROR.getDesc());
        }
        logger.error("expectedException|预期错误|{},{}", ex.getMessage(), ex);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return handleExceptionInternal(ex, JSON.toJSONString(exceptionResponse), httpHeaders
                , HttpStatus.BAD_REQUEST, request);
    }
}