package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_logistics_trace")
public class LogisticsTrace {
    @TableId(value = "trace_id", type = IdType.AUTO)
    private Long id;

    @TableField("logistics_id")
    private Long logisticsId;

    @TableField("trace_content")
    private String traceContent;

    @TableField("trace_time")
    private LocalDateTime traceTime;

    @TableField("trace_location")
    private String traceLocation;
}
