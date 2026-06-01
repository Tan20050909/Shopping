package com.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_banner")
public class Banner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String image;
    private String link;
    private Integer sort;
    private Integer status;
    private String bgColor;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
