package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("tb_dashboard_metric")
public class DashboardMetric {
    @TableId(type = IdType.AUTO)
    private Long metricId;
    private String metricCode;
    private BigDecimal metricValue;
    private LocalDate metricDate;
    private Integer metricHour;
    private LocalDateTime createTime;
}
