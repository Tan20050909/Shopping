package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@TableName("tb_after_sale")
public class AfterSale {
    @TableId(type = IdType.AUTO)
    private Long afterSaleId;
    private String afterSaleNo;
    private Long groupId;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long merchantId;
    private Integer afterSaleType;
    private String applyReason;
    private String applyEvidence;
    private BigDecimal applyAmount;
    private Integer handleStatus;
    private String merchantRemark;
    private String platformRemark;
    @TableField(exist = false)
    private Long handleAdminId;
    private LocalDateTime handleTime;
    private LocalDateTime applyTime;
    private LocalDateTime updateTime;
}
