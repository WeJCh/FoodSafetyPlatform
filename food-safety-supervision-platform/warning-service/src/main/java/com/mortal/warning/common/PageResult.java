package com.mortal.warning.common;

import java.util.List;
import lombok.Data;

/**
 * 通用分页结果。
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private int page;
    private int size;
    private long pages;

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        result.setPages(pages);
        return result;
    }
}
