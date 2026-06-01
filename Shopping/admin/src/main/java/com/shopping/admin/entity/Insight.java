package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_insight")
public class Insight {
    @TableId(type = IdType.AUTO)
    private Long insightId;
    private String insightType;
    private String title;
    private String content;
    /** 严重程度：1-提示 2-警告 3-紧急 */
    private Integer severity;
    private String relatedModule;
    private Long relatedId;
    /** 处理状态：0-待处理 1-已处理 2-已忽略 */
    private Integer handleStatus;
    private Long handleAdminId;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
