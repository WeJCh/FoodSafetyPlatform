package com.mortal.regulation.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        if ("unauthorized".equalsIgnoreCase(ex.getMessage())) {
            return ApiResponse.failure(401, "unauthorized");
        }
        return ApiResponse.failure(400, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        return ApiResponse.failure(400, ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }
}
