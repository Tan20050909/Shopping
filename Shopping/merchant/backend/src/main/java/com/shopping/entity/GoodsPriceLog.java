package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods_price_log")
public class GoodsPriceLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private LocalDateTime createTime;
}
