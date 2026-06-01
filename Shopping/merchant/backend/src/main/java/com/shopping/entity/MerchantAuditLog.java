package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_merchant_audit_log")
public class MerchantAuditLog {
    @TableId(value = "audit_log_id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("before_status")
    private Integer beforeStatus;

    @TableField("after_status")
    private Integer afterStatus;

    @TableField("audit_admin_id")
    private Long auditAdminId;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("create_time")
    private LocalDateTime createTime;
}

