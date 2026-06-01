package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_arbitration")
public class Arbitration {
    @TableId(type = IdType.AUTO)
    private Long arbitrationId;
    private Long disputeId;
    /** 状态：0-待仲裁 1-仲裁中 2-已裁决 */
    private Integer arbitrationStatus;
    private String arbitratorIds;
    private String voteResults;
    private String finalResult;
    private LocalDateTime deadline;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}
