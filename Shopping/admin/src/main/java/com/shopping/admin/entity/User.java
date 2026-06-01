package com.shopping.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_user")
public class User {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    @TableField("real_name")
    private String realName;
    private String password;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDateTime birthday;
    @TableField("id_card")
    private String idCard;
    @TableField("auth_status")
    private Integer authStatus;
    @TableField("login_ip")
    private String loginIp;
    @TableField("register_time")
    private LocalDateTime registerTime;
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
    // 以下字段在 tb_user 中不存在，保留供其他查询使用
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String email;
    @TableField(exist = false)
    private Integer creditScore;
    @TableField(exist = false)
    private Integer userLevel;
    @TableField(exist = false)
    private String riskTag;
    @TableField(exist = false)
    private BigDecimal totalSpent;
    @TableField(exist = false)
    private Integer orderCount;
    @TableField(exist = false)
    private String registerIp;
    @TableField(exist = false)
    private String lastLoginIp;
    @TableField(exist = false)
    private Integer realNameVerified;
}
