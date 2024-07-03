package com.pig4cloud.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface Response {
    int getCode();

    void setCode(int code);

    String getMessage();

    void setMessage(String message);

    Object getData();

    void setData(Object data);

    void  pagination(Page<?> rowPage);
}
