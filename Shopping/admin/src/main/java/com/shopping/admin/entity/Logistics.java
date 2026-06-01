package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_logistics")
public class Logistics {
    @TableId(value = "logistics_id", type = IdType.AUTO)
    private Long logisticsId;
    @TableField("order_id")
    private Long orderId;
    @TableField("merchant_id")
    private Long merchantId;
    @TableField("biz_type")
    private Integer bizType;
    @TableField("express_company")
    private String expressCompany;
    @TableField("express_no")
    private String expressNo;
    @TableField("logistics_status")
    private Integer logisticsStatus;
    @TableField("predict_receive_time")
    private LocalDateTime predictReceiveTime;
    @TableField("sign_time")
    private LocalDateTime signTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
