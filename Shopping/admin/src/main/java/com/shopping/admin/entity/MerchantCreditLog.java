package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant_credit_log")
public class MerchantCreditLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long merchantId;
    private Integer scoreChange;
    private Integer scoreBefore;
    private Integer scoreAfter;
    private String changeReason;
    private String dimension;
    private Long operatorId;
    private LocalDateTime createTime;
}
