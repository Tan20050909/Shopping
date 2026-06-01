package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_logistics_trace")
public class LogisticsTrace {
    @TableId(value = "trace_id", type = IdType.AUTO)
    private Long traceId;
    @TableField("logistics_id")
    private Long logisticsId;
    @TableField("trace_content")
    private String traceContent;
    @TableField("trace_time")
    private LocalDateTime traceTime;
    @TableField("trace_location")
    private String traceLocation;
}
