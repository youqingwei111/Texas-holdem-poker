package com.poker.common;

import lombok.Data;

/**
 * 统一返回结果
 */
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public Result() {}

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> error() {
        return new Result<>(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage(), null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ErrorCode.SYSTEM_ERROR.getCode(), message, null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return ErrorCode.SUCCESS.getCode().equals(this.code);
    }
}