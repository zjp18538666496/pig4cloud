package com.pig4cloud.common.exception;

import com.pig4cloud.common.result.R;
import lombok.Getter;

/**
 * 业务异常，由GlobalExceptionHandler转换为R.fail响应
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(R.FAIL, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
