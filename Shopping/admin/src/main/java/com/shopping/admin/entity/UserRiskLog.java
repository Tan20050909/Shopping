package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_user_risk_log")
public class UserRiskLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long userId;
    private String riskType;
    private String riskDesc;
    private Integer riskLevel;
    private String evidence;
    private Integer handleStatus;
    private Long handleAdminId;
    private String handleResult;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
