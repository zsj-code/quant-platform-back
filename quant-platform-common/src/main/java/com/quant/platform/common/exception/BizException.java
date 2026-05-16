package com.quant.platform.common.exception;


import com.quant.platform.common.api.ResultCode;

public class BizException extends RuntimeException {
    private final int code;

    public BizException(ResultCode code, String message) {
        super(message);
        this.code = code.code();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
