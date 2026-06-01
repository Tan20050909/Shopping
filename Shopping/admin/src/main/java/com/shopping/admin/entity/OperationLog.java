package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_operation_log")
public class OperationLog {
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    @TableField("operator_type")
    private Integer operatorType;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operation_module")
    private String operationModule;

    @TableField("operation_content")
    private String operationContent;

    @TableField("operation_ip")
    private String operationIp;

    @TableField("operation_time")
    private LocalDateTime operationTime;

    @TableField("operation_result")
    private Integer operationResult;

    // 以下字段在 tb_operation_log 中不存在，仅供 Java 层使用
    // 如需持久化，请拼接进 operation_content
    @TableField(exist = false)
    private String adminName;
    @TableField(exist = false)
    private String requestUrl;
    @TableField(exist = false)
    private String requestMethod;
    @TableField(exist = false)
    private String requestParams;
    @TableField(exist = false)
    private String errorMsg;
}
