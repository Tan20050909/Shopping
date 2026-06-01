package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_goods_pic")
public class GoodsPic {
    @TableId(value = "pic_id", type = IdType.AUTO)
    private Long picId;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("pic_url")
    private String picUrl;

    @TableField("pic_sort")
    private Integer picSort;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;
}
