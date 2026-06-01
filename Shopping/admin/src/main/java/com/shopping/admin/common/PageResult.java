package com.shopping.admin.common;

import lombok.Data;

@Data
public class PageResult<T> {
    private java.util.List<T> records;
    private long total;
    private long pages;
    private long current;
    private long size;

    public PageResult(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        this.records = page.getRecords();
        this.total = page.getTotal();
        this.pages = page.getPages();
        this.current = page.getCurrent();
        this.size = page.getSize();
    }

    public PageResult(java.util.List<T> records, long total) {
        this.records = records;
        this.total = total;
        this.pages = total > 0 ? 1 : 0;
        this.current = 1;
        this.size = records != null ? records.size() : 0;
    }
}
