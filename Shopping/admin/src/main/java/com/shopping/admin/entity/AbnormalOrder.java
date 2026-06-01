package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_abnormal_order")
public class AbnormalOrder {
    @TableId(type = IdType.AUTO)
    private Long abnormalId;
    private Long orderId;
    private Integer abnormalType;
    private String abnormalDesc;
    private Integer handleStatus;
    private Long handleAdminId;
    private String handleRemark;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
