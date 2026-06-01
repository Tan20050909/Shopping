package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant")
public class Merchant {
    @TableId(type = IdType.AUTO)
    private Long merchantId;
    private String merchantName;
    /** 商家类型：1-个人商户 2-企业商户 */
    private Integer merchantType;
    private String legalPerson;
    private String idCard;
    private String businessLicense;
    private String industryLicense;
    private String phone;
    private String email;
    private String address;
    private String password;
    private String shopLogo;
    private String shopIntro;
    private Integer auditStatus;
    private String auditRemark;
    private java.math.BigDecimal shopScore;
    /** 店铺状态：0-未开业 1-营业中 2-停业 3-被封 */
    private Integer status;
    private LocalDateTime registerTime;
    private LocalDateTime auditTime;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime updateTime;
}
