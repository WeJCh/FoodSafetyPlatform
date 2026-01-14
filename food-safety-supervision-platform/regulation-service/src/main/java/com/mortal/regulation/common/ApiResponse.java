package com.mortal.regulation.common;

import java.time.Instant;
import lombok.Data;

@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(0);
        response.setMessage("ok");
        response.setData(data);
        response.setTimestamp(Instant.now());
        return response;
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(Instant.now());
        return response;
    }
}
