package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("tb_order")
public class Order {
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("group_id")
    private Long groupId;

    @TableField("group_no")
    private String groupNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("user_id")
    private Long userId;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @TableField("freight")
    private BigDecimal freight;

    @TableField("pay_status")
    private Integer payStatus;

    @TableField("order_status")
    private Integer status;

    @TableField("addr_id")
    private Long addrId;

    @TableField("consignee")
    private String consignee;

    @TableField("consignee_phone")
    private String consigneePhone;

    @TableField("receive_addr")
    private String receiveAddr;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("delivery_time")
    private LocalDateTime deliveryTime;

    @TableField("receive_time")
    private LocalDateTime receiveTime;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("buyer_remark")
    private String buyerRemark;

    @TableField("merchant_remark")
    private String merchantRemark;

    @TableField(exist = false)
    private String logisticsNo;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<String> itemPics;

    @TableField(exist = false)
    private List<String> itemNames;

    @TableField(exist = false)
    private Integer afterSaleCount;

    @TableField(exist = false)
    private Boolean afterSaleDone;

    @TableField(exist = false)
    private String userNickname;

    @TableField(exist = false)
    private String userAvatar;
}
