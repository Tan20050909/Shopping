package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods_audit_log")
public class GoodsAuditLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long goodsId;
    private Long merchantId;
    private Integer auditStatus;
    private String auditRemark;
    private Long auditAdminId;
    private LocalDateTime createTime;
}
