package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_system_config")
public class SystemConfig {
    @TableId(type = IdType.AUTO)
    private Long configId;
    private String configKey;
    private String configValue;
    private String configDesc;
    private String configGroup;
    private Integer sortNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
