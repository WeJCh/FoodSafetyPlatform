package com.mortal.warning.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理非法参数异常
     * @param ex 非法参数异常
     * @return ApiResponse<Void>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.failure(400, ex.getMessage());
    }
    /**
     * 处理方法参数验证异常
     * @param ex 方法参数验证异常
     * @return ApiResponse<Void>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        return ApiResponse.failure(400, ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }
}
