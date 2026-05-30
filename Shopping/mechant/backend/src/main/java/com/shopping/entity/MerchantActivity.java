package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_merchant_activity")
public class MerchantActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long platformActivityId;
    private Integer status;
    private LocalDateTime createTime;
}
