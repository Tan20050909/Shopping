package org.example.common;

import java.util.List;

public record PageResult<T>(int pageNum, int pageSize, long total, List<T> records) {
    public PageResult(long total, int pageNum, int pageSize, List<T> records) {
        this(pageNum, pageSize, total, records);
    }
}
