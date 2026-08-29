package com.pig4cloud.common.result;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 分页数据，字段结构({rows,total,pages,size,current})与前端表格约定保持一致
 */
@Getter
@Setter
public class PageResult<T> {

    private List<T> rows;
    private long total;
    private long pages;
    private long size;
    private long current;

    public static <T> PageResult<T> of(List<T> rows, long total, long pageSize, long page) {
        PageResult<T> result = new PageResult<>();
        result.rows = rows == null ? List.of() : rows;
        result.total = total;
        result.size = pageSize;
        result.current = page;
        result.pages = pageSize > 0 ? (long) Math.ceil((double) total / pageSize) : 0;
        return result;
    }
}
