package com.heizi.ai.handler;

import cn.hutool.core.util.StrUtil;
import com.heizi.common.enums.ResultCode;
import com.heizi.common.exception.BusinessException;
import com.heizi.common.result.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        log.warn("业务异常，method={}, uri={}, code={}, message={}",
                request.getMethod(), buildRequestUri(request), exception.getCode(), exception.getMessage());
        return ApiResponse.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception, HttpServletRequest request) {
        log.error("系统异常，method={}, uri={}", request.getMethod(), buildRequestUri(request), exception);
        return ApiResponse.fail(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMessage());
    }

    private String buildRequestUri(HttpServletRequest request) {
        if (StrUtil.isBlank(request.getQueryString())) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + request.getQueryString();
    }

}
