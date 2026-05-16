package com.quant.platform.common.api;

public enum ResultCode {
    OK(0, "OK"), BAD_REQUEST(400, "请求参数错误"), UNAUTHORIZED(401, "未认证"), FORBIDDEN(403, "无权限"), NOT_FOUND(404,
            "资源不存在"), CONFLICT(409, "请求冲突"), INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String defaultMessage;

    ResultCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
