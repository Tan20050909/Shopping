package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_order")
public class Order {
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;
    @TableField("order_no")
    private String orderNo;
    @TableField("group_id")
    private Long groupId;
    @TableField("group_no")
    private String groupNo;
    @TableField("user_id")
    private Long userId;
    @TableField("merchant_id")
    private Long merchantId;
    @TableField("total_amount")
    private BigDecimal totalAmount;
    @TableField("pay_amount")
    private BigDecimal payAmount;
    @TableField(exist = false)
    private BigDecimal actualAmount;
    @TableField("discount_amount")
    private BigDecimal discountAmount;
    private BigDecimal freight;
    @TableField("pay_type")
    private Integer payType;
    @TableField("pay_status")
    private Integer payStatus;
    @TableField("order_status")
    private Integer orderStatus;
    @TableField("addr_id")
    private Long addrId;
    private String consignee;
    @TableField("consignee_phone")
    private String consigneePhone;
    @TableField("receive_addr")
    private String receiveAddr;
    @TableField("pay_time")
    private LocalDateTime payTime;
    @TableField(exist = false)
    private LocalDateTime shipTime;
    @TableField("delivery_time")
    private LocalDateTime deliveryTime;
    @TableField("receive_time")
    private LocalDateTime receiveTime;
    @TableField("cancel_time")
    private LocalDateTime cancelTime;
    @TableField("expire_time")
    private LocalDateTime expireTime;
    @TableField(exist = false)
    private LocalDateTime refundTime;
    @TableField(exist = false)
    private BigDecimal refundAmount;
    @TableField(exist = false)
    private String refundReason;
    @TableField("buyer_remark")
    private String buyerRemark;
    @TableField("merchant_remark")
    private String merchantRemark;
    @TableField(exist = false)
    private String adminNote;
    @TableField(exist = false)
    private String abnormalTags;
    @TableField(exist = false)
    private Integer riskLevel;
    @TableField(exist = false)
    private String trackingNo;
    @TableField(exist = false)
    private String shippingCompany;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private Integer isDeleted;
}
