package com.quant.platform.common.api;


public class Result<T> {
    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.OK.code(), ResultCode.OK.defaultMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.OK.code(), ResultCode.OK.defaultMessage(), data);
    }

    public static <T> Result<T> fail(ResultCode code) {
        return new Result<>(code.code(), code.defaultMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode code, String message) {
        return new Result<>(code.code(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
