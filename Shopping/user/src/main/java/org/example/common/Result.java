package org.example.common;

public record Result<T>(String code, String message, T data) {
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS, "success", data);
    }

    public static Result<Void> ok() {
        return new Result<>(ErrorCode.SUCCESS, "success", null);
    }

    public static Result<Void> fail(String message) {
        return new Result<>(ErrorCode.SYSTEM_ERROR, message, null);
    }

    public static Result<Void> fail(String code, String message) {
        return new Result<>(code, message, null);
    }
}
