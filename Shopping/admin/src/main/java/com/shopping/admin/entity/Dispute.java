package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_dispute")
public class Dispute {
    @TableId(type = IdType.AUTO)
    private Long disputeId;
    /** 纠纷单号 */
    private String disputeNo;
    /** 关联售后单ID */
    private Long afterSaleId;
    /** 父订单ID */
    private Long groupId;
    /** 子订单ID */
    private Long orderId;
    /** 订单项ID */
    private Long orderItemId;
    private Long userId;
    private Long merchantId;
    /** 发起方：1-用户 2-商家 3-平台 */
    private Integer applyType;
    /** 纠纷原因 */
    private String disputeReason;
    /** 纠纷详细说明 */
    private String disputeDesc;
    /** 纠纷状态：0-待平台处理 1-举证中 2-平台处理中 3-已裁决 4-已关闭 */
    private Integer disputeStatus;
    /** 判责结果：1-支持用户 2-支持商家 3-部分支持 4-双方协商关闭 */
    private Integer judgeResult;
    /** 最终退款或补偿金额 */
    private BigDecimal finalAmount;
    /** 处理平台管理员ID */
    private Long platformAdminId;
    /** 平台处理意见 */
    private String platformOpinion;
    private LocalDateTime createTime;
    private LocalDateTime judgeTime;
    private LocalDateTime updateTime;
}
