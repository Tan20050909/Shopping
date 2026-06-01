package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_logistics")
public class Logistics {
    @TableId(value = "logistics_id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("biz_type")
    private Integer bizType;

    @TableField("express_company")
    private String company;

    @TableField("express_no")
    private String trackingNo;

    @TableField("logistics_status")
    private Integer status;

    @TableField("predict_receive_time")
    private LocalDateTime predictReceiveTime;

    @TableField("sign_time")
    private LocalDateTime signTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
