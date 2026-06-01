package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant")
public class Merchant {
    @TableId(value = "merchant_id", type = IdType.AUTO)
    private Long merchantId;

    @TableField("merchant_name")
    private String merchantName;

    @TableField("merchant_type")
    private Integer merchantType;

    @TableField("legal_person")
    private String legalPerson;

    @TableField("id_card")
    private String idCard;

    @TableField("business_license")
    private String businessLicense;

    @TableField("industry_license")
    private String industryLicense;

    private String phone;

    private String email;

    private String address;

    private String password;

    @TableField("shop_logo")
    private String shopLogo;

    @TableField("shop_intro")
    private String shopIntro;

    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("shop_score")
    private Double shopScore;

    private Integer status;

    @TableField("register_time")
    private LocalDateTime registerTime;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Long followerCount;

    @TableField(exist = false)
    private Boolean followed;
}
