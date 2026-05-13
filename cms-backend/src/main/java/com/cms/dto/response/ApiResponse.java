package com.cms.dto.response;

public record ApiResponse<T>(T data, String message, boolean success) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, "ok", true);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, message, false);
    }
}
