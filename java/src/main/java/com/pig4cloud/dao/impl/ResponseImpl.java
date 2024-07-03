package com.pig4cloud.dao.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.dao.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * 数据分页
     */
    /**
     * 数据分页
     */
    public void pagination(Page<?> rowPage) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("rows", rowPage.getRecords());
        dataMap.put("total", rowPage.getTotal());
        dataMap.put("size", rowPage.getSize());
        dataMap.put("current", rowPage.getCurrent());
        dataMap.put("pages", rowPage.getPages());
        this.setData(dataMap);
    }
}

