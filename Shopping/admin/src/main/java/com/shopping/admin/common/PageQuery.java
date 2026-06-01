package com.shopping.admin.common;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class PageQuery {
    private long current = 1;
    private long size = 10;
    private String keyword;
}
