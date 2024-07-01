package com.pig4cloud.dao.impl;

import com.pig4cloud.dao.Response;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseImpl implements Response {
    private int code;
    private String message;
    private Object data;

    public ResponseImpl(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}

