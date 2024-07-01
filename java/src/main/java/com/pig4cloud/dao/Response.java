package com.pig4cloud.dao;

public interface Response {
    int getCode();

    void setCode(int code);

    String getMessage();

    void setMessage(String message);

    Object getData();

    void setData(Object data);
}
