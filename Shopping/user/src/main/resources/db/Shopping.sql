-- noinspection SqlNoDataSourceInspection,SqlDialectInspection
-- Shopping 电商平台完整数据库结构
-- 说明：本文件包含建库、建表 SQL 与一套合理的演示测试数据。
-- 设计目标：支撑用户端、商家端、平台管理端三端完整业务，包括父子订单、支付退款、优惠券、售后、库存流水、财务流水、消息、首页运营位、统计看板与权限体系。

CREATE DATABASE IF NOT EXISTS `Shopping`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `Shopping`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `tb_chat_message`;
DROP TABLE IF EXISTS `tb_chat_session`;
DROP TABLE IF EXISTS `tb_merchant_ai_rule`;
DROP TABLE IF EXISTS `tb_dispute_evidence`;
DROP TABLE IF EXISTS `tb_dispute`;
DROP TABLE IF EXISTS `tb_merchant_push_task`;
DROP TABLE IF EXISTS `tb_goods_daily_stat`;
DROP TABLE IF EXISTS `tb_merchant_daily_stat`;
DROP TABLE IF EXISTS `tb_platform_daily_stat`;
DROP TABLE IF EXISTS `tb_home_section_goods`;
DROP TABLE IF EXISTS `tb_home_section`;
DROP TABLE IF EXISTS `tb_banner`;
DROP TABLE IF EXISTS `tb_live_watch_record`;
DROP TABLE IF EXISTS `tb_live_like`;
DROP TABLE IF EXISTS `tb_live_comment`;
DROP TABLE IF EXISTS `tb_live_goods`;
DROP TABLE IF EXISTS `tb_live`;
DROP TABLE IF EXISTS `tb_goods_rank_snapshot`;
DROP TABLE IF EXISTS `tb_recommend_record`;
DROP TABLE IF EXISTS `tb_hot_search_keyword`;
DROP TABLE IF EXISTS `tb_search_history`;
DROP TABLE IF EXISTS `tb_user_browse_history`;
DROP TABLE IF EXISTS `tb_user_collect`;
DROP TABLE IF EXISTS `tb_shop_follow`;
DROP TABLE IF EXISTS `tb_order_coupon`;
DROP TABLE IF EXISTS `tb_user_coupon`;
DROP TABLE IF EXISTS `tb_coupon_scope`;
DROP TABLE IF EXISTS `tb_coupon`;
DROP TABLE IF EXISTS `tb_after_sale_logistics`;
DROP TABLE IF EXISTS `tb_after_sale_log`;
DROP TABLE IF EXISTS `tb_after_sale`;
DROP TABLE IF EXISTS `tb_logistics_trace`;
DROP TABLE IF EXISTS `tb_logistics`;
DROP TABLE IF EXISTS `tb_refund`;
DROP TABLE IF EXISTS `tb_payment`;
DROP TABLE IF EXISTS `tb_order_status_log`;
DROP TABLE IF EXISTS `tb_order_item`;
DROP TABLE IF EXISTS `tb_order`;
DROP TABLE IF EXISTS `tb_order_group`;
DROP TABLE IF EXISTS `tb_user_cart`;
DROP TABLE IF EXISTS `tb_goods_comment`;
DROP TABLE IF EXISTS `tb_sku_stock_log`;
DROP TABLE IF EXISTS `tb_goods_price_log`;
DROP TABLE IF EXISTS `tb_goods_pic`;
DROP TABLE IF EXISTS `tb_goods_sku`;
DROP TABLE IF EXISTS `tb_goods_audit_log`;
DROP TABLE IF EXISTS `tb_goods`;
DROP TABLE IF EXISTS `tb_category`;
DROP TABLE IF EXISTS `tb_merchant_activity`;
DROP TABLE IF EXISTS `tb_platform_activity`;
DROP TABLE IF EXISTS `tb_platform_notice`;
DROP TABLE IF EXISTS `tb_message`;
DROP TABLE IF EXISTS `tb_operation_log`;
DROP TABLE IF EXISTS `tb_violation`;
DROP TABLE IF EXISTS `tb_abnormal_order`;
DROP TABLE IF EXISTS `tb_merchant_account_flow`;
DROP TABLE IF EXISTS `tb_merchant_withdraw`;
DROP TABLE IF EXISTS `tb_merchant_finance`;
DROP TABLE IF EXISTS `tb_merchant_setting`;
DROP TABLE IF EXISTS `tb_merchant_audit_log`;
DROP TABLE IF EXISTS `tb_merchant`;
DROP TABLE IF EXISTS `tb_role_permission_relation`;
DROP TABLE IF EXISTS `tb_admin_role_relation`;
DROP TABLE IF EXISTS `tb_permission`;
DROP TABLE IF EXISTS `tb_role`;
DROP TABLE IF EXISTS `tb_platform_admin`;
DROP TABLE IF EXISTS `tb_user_asset`;
DROP TABLE IF EXISTS `tb_user_address`;
DROP TABLE IF EXISTS `tb_user`;

CREATE TABLE `tb_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
  `phone` varchar(11) NOT NULL COMMENT '手机号，唯一登录账号',
  `password` varchar(100) NOT NULL COMMENT '密码哈希，建议使用BCrypt',
  `nickname` varchar(32) NOT NULL COMMENT '用户昵称',
  `avatar` varchar(255) DEFAULT '/default/avatar.png' COMMENT '用户头像地址',
  `real_name` varchar(20) DEFAULT NULL COMMENT '真实姓名，实名认证使用',
  `id_card` varchar(64) DEFAULT NULL COMMENT '身份证号密文或脱敏值',
  `auth_status` tinyint NOT NULL DEFAULT 0 COMMENT '实名认证状态：0-未认证 1-已认证 2-审核中 3-审核失败',
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP，兼容IPv4和IPv6',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常 0-禁用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_auth_status` (`auth_status`),
  KEY `idx_status_register` (`status`,`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基础信息表';

CREATE TABLE `tb_user_asset` (
  `asset_id` bigint NOT NULL AUTO_INCREMENT COMMENT '资产ID，主键',
  `user_id` bigint NOT NULL COMMENT '所属用户ID，唯一',
  `integral` int NOT NULL DEFAULT 0 COMMENT '可用积分',
  `integral_expire_time` datetime DEFAULT NULL COMMENT '积分过期时间',
  `coupon_num` int NOT NULL DEFAULT 0 COMMENT '可用优惠券数量，冗余统计',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`asset_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资产汇总表';

CREATE TABLE `tb_user_address` (
  `addr_id` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID，主键',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `consignee` varchar(20) NOT NULL COMMENT '收货人',
  `phone` varchar(11) NOT NULL COMMENT '收货电话',
  `province` varchar(32) NOT NULL COMMENT '省份',
  `province_code` varchar(12) DEFAULT NULL COMMENT '省份行政区划代码',
  `city` varchar(32) NOT NULL COMMENT '城市',
  `city_code` varchar(12) DEFAULT NULL COMMENT '城市行政区划代码',
  `district` varchar(32) NOT NULL COMMENT '区县',
  `district_code` varchar(12) DEFAULT NULL COMMENT '区县行政区划代码',
  `detail_addr` varchar(255) NOT NULL COMMENT '详细地址',
  `postal_code` varchar(10) DEFAULT NULL COMMENT '邮政编码',
  `remark` varchar(100) DEFAULT NULL COMMENT '备注，如送货时间或楼层',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否 1-是',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`addr_id`),
  KEY `idx_user_default` (`user_id`,`is_default`),
  KEY `idx_user_deleted` (`user_id`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收货地址表';

CREATE TABLE `tb_platform_admin` (
  `admin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID，主键',
  `admin_name` varchar(32) NOT NULL COMMENT '管理员账号，唯一',
  `password` varchar(100) NOT NULL COMMENT '密码哈希，建议使用BCrypt',
  `real_name` varchar(20) NOT NULL COMMENT '真实姓名',
  `phone` varchar(11) NOT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常 0-禁用',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP，兼容IPv4和IPv6',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `uk_admin_name` (`admin_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台管理员表';

CREATE TABLE `tb_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID，主键',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码，唯一，如SUPER_ADMIN、OPERATOR',
  `role_name` varchar(32) NOT NULL COMMENT '角色名称',
  `role_type` tinyint NOT NULL DEFAULT 3 COMMENT '角色类型：1-用户 2-商家 3-平台管理员',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_role_type_status` (`role_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE `tb_permission` (
  `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID，主键',
  `permission_code` varchar(100) NOT NULL COMMENT '权限编码，唯一，如goods:audit',
  `permission_name` varchar(64) NOT NULL COMMENT '权限名称',
  `permission_type` tinyint NOT NULL COMMENT '权限类型：1-菜单 2-按钮 3-接口',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父权限ID，0表示顶级',
  `path` varchar(200) DEFAULT NULL COMMENT '前端路由或后端接口路径',
  `method` varchar(10) DEFAULT NULL COMMENT 'HTTP方法：GET/POST/PUT/DELETE等',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_type_status` (`permission_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限点表';

CREATE TABLE `tb_admin_role_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID，主键',
  `admin_id` bigint NOT NULL COMMENT '管理员ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role` (`admin_id`,`role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员角色关联表';

CREATE TABLE `tb_role_permission_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID，主键',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';

CREATE TABLE `tb_merchant` (
  `merchant_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商家ID，主键',
  `merchant_name` varchar(64) NOT NULL COMMENT '店铺名称，唯一',
  `merchant_type` tinyint NOT NULL COMMENT '商家类型：1-个人商户 2-企业商户',
  `legal_person` varchar(20) NOT NULL COMMENT '法人或经营者姓名',
  `id_card` varchar(64) NOT NULL COMMENT '身份证号密文或脱敏值',
  `business_license` varchar(255) DEFAULT NULL COMMENT '营业执照图片地址，企业商户必传',
  `industry_license` varchar(255) DEFAULT NULL COMMENT '行业许可证地址，特殊品类使用',
  `phone` varchar(11) NOT NULL COMMENT '商家联系电话',
  `email` varchar(64) DEFAULT NULL COMMENT '商家邮箱',
  `address` varchar(255) DEFAULT NULL COMMENT '商家实体地址',
  `password` varchar(100) NOT NULL COMMENT '密码哈希，建议使用BCrypt',
  `shop_logo` varchar(255) DEFAULT '/default/shop_logo.png' COMMENT '店铺LOGO',
  `shop_intro` varchar(500) DEFAULT NULL COMMENT '店铺简介',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '入驻审核状态：0-待审核 1-通过 2-失败',
  `audit_remark` varchar(200) DEFAULT NULL COMMENT '审核意见',
  `shop_score` decimal(2,1) DEFAULT 5.0 COMMENT '店铺综合评分，0-5分',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '店铺状态：0-未开业 1-营业中 2-停业 3-被封',
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入驻申请时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核通过时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`merchant_id`),
  UNIQUE KEY `uk_merchant_name` (`merchant_name`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_status_type` (`status`,`merchant_type`),
  KEY `idx_shop_score` (`shop_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家基础信息表';

CREATE TABLE `tb_merchant_audit_log` (
  `audit_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商家审核日志ID，主键',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `before_status` tinyint DEFAULT NULL COMMENT '审核前状态',
  `after_status` tinyint NOT NULL COMMENT '审核后状态：0-待审核 1-通过 2-失败',
  `audit_admin_id` bigint NOT NULL COMMENT '审核管理员ID',
  `audit_remark` varchar(200) DEFAULT NULL COMMENT '审核意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`audit_log_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_admin_id` (`audit_admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家入驻审核日志表';

CREATE TABLE `tb_merchant_setting` (
  `setting_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设置ID，主键',
  `merchant_id` bigint NOT NULL COMMENT '所属商家ID，唯一',
  `business_hours` varchar(50) DEFAULT '00:00-24:00' COMMENT '营业时间',
  `refund_time` tinyint NOT NULL DEFAULT 24 COMMENT '退款处理时限，单位小时',
  `exchange_time` tinyint NOT NULL DEFAULT 48 COMMENT '换货处理时限，单位小时',
  `freight_template` text NOT NULL COMMENT '运费模板，JSON格式',
  `ai_reply_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用AI自动回复：0-否 1-是',
  `ai_resume_minutes` int NOT NULL DEFAULT 30 COMMENT 'AI自动回复暂停后恢复分钟数',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`setting_id`),
  UNIQUE KEY `uk_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家店铺设置表';

CREATE TABLE `tb_merchant_ai_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI自动回复规则ID，主键',
  `merchant_id` bigint NOT NULL COMMENT '所属商家ID',
  `rule_name` varchar(64) NOT NULL COMMENT '规则名称',
  `match_type` tinyint NOT NULL DEFAULT 1 COMMENT '匹配方式：1-关键词包含 2-完全匹配 3-正则匹配 4-营业时间外默认回复',
  `keywords` varchar(255) DEFAULT NULL COMMENT '触发关键词，多个关键词用逗号分隔',
  `reply_content` varchar(1000) NOT NULL COMMENT '自动回复内容',
  `priority` int NOT NULL DEFAULT 0 COMMENT '优先级，数值越大越优先',
  `work_time_start` time DEFAULT NULL COMMENT '生效开始时间，空表示全天',
  `work_time_end` time DEFAULT NULL COMMENT '生效结束时间，空表示全天',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-停用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  KEY `idx_merchant_status` (`merchant_id`,`status`),
  KEY `idx_priority` (`merchant_id`,`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家AI自动回复规则表';

CREATE TABLE `tb_merchant_finance` (
  `finance_id` bigint NOT NULL AUTO_INCREMENT COMMENT '财务ID，主键',
  `merchant_id` bigint NOT NULL COMMENT '商家ID，唯一',
  `shop_balance` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '店铺可用余额',
  `unsettle_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '未结算金额',
  `commission_rate` decimal(5,4) NOT NULL DEFAULT 0.0500 COMMENT '平台佣金比例，默认5%',
  `freeze_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`finance_id`),
  UNIQUE KEY `uk_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家财务汇总表';

CREATE TABLE `tb_merchant_withdraw` (
  `withdraw_id` bigint NOT NULL AUTO_INCREMENT COMMENT '提现ID，主键',
  `withdraw_no` varchar(32) NOT NULL COMMENT '提现单号，唯一',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `withdraw_amount` decimal(10,2) NOT NULL COMMENT '提现金额',
  `bank_name` varchar(32) NOT NULL COMMENT '开户银行',
  `bank_card` varchar(64) NOT NULL COMMENT '银行卡号密文或脱敏值',
  `account_name` varchar(20) NOT NULL COMMENT '开户人姓名',
  `withdraw_status` tinyint NOT NULL DEFAULT 0 COMMENT '提现状态：0-待审核 1-审核通过 2-提现成功 3-审核失败 4-已取消',
  `audit_admin_id` bigint DEFAULT NULL COMMENT '审核管理员ID',
  `audit_remark` varchar(200) DEFAULT NULL COMMENT '审核意见',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `success_time` datetime DEFAULT NULL COMMENT '到账时间',
  PRIMARY KEY (`withdraw_id`),
  UNIQUE KEY `uk_withdraw_no` (`withdraw_no`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_withdraw_status` (`withdraw_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家提现申请表';

CREATE TABLE `tb_merchant_account_flow` (
  `flow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '资金流水ID，主键',
  `flow_no` varchar(32) NOT NULL COMMENT '资金流水号，唯一',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `order_id` bigint DEFAULT NULL COMMENT '关联子订单ID',
  `refund_id` bigint DEFAULT NULL COMMENT '关联退款单ID',
  `withdraw_id` bigint DEFAULT NULL COMMENT '关联提现单ID',
  `flow_type` tinyint NOT NULL COMMENT '流水类型：1-订单入账 2-平台佣金 3-退款扣减 4-提现冻结 5-提现成功 6-提现退回 7-人工调整',
  `amount` decimal(12,2) NOT NULL COMMENT '变动金额，收入为正，支出为负',
  `balance_after` decimal(12,2) NOT NULL COMMENT '变动后可用余额',
  `freeze_after` decimal(12,2) DEFAULT NULL COMMENT '变动后冻结金额',
  `remark` varchar(200) DEFAULT NULL COMMENT '流水说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`flow_id`),
  UNIQUE KEY `uk_flow_no` (`flow_no`),
  KEY `idx_merchant_time` (`merchant_id`,`create_time`),
  KEY `idx_flow_type` (`flow_type`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_refund_id` (`refund_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家资金流水表';

CREATE TABLE `tb_category` (
  `cate_id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID，主键',
  `parent_cate_id` int NOT NULL DEFAULT 0 COMMENT '父分类ID：0-一级分类，大于0表示子分类',
  `cate_name` varchar(32) NOT NULL COMMENT '分类名称',
  `cate_sort` int NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`cate_id`),
  KEY `idx_parent_cate` (`parent_cate_id`),
  KEY `idx_cate_sort_status` (`cate_sort`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';

CREATE TABLE `tb_goods` (
  `goods_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID，主键',
  `merchant_id` bigint NOT NULL COMMENT '所属商家ID',
  `cate_id` int NOT NULL COMMENT '商品分类ID',
  `goods_name` varchar(128) NOT NULL COMMENT '商品名称，搜索核心字段',
  `goods_intro` varchar(500) DEFAULT NULL COMMENT '商品简介',
  `goods_pic` varchar(255) NOT NULL COMMENT '商品主图地址',
  `goods_video` varchar(255) DEFAULT NULL COMMENT '详情视频地址',
  `keywords` varchar(100) DEFAULT NULL COMMENT '搜索关键词，逗号分隔',
  `ship_from` varchar(128) DEFAULT NULL COMMENT '发货地',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核 1-通过 2-驳回',
  `audit_remark` varchar(200) DEFAULT NULL COMMENT '审核意见',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '商品状态：0-下架 1-上架 2-预售 3-违规下架',
  `sell_count` bigint NOT NULL DEFAULT 0 COMMENT '累计销量',
  `comment_count` int NOT NULL DEFAULT 0 COMMENT '累计评论数',
  `goods_score` decimal(2,1) DEFAULT 5.0 COMMENT '商品评分，0-5分',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`goods_id`),
  KEY `idx_merchant_status` (`merchant_id`,`status`),
  KEY `idx_cate_status` (`cate_id`,`status`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_goods_name` (`goods_name`),
  KEY `idx_sell_count` (`sell_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品SPU基础信息表';

CREATE TABLE `tb_goods_audit_log` (
  `audit_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品审核日志ID，主键',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `before_status` tinyint DEFAULT NULL COMMENT '审核前状态',
  `after_status` tinyint NOT NULL COMMENT '审核后状态：0-待审核 1-通过 2-驳回',
  `audit_admin_id` bigint NOT NULL COMMENT '审核管理员ID',
  `audit_remark` varchar(200) DEFAULT NULL COMMENT '审核意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`audit_log_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_admin_id` (`audit_admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品审核日志表';

CREATE TABLE `tb_goods_sku` (
  `sku_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SKU ID，主键',
  `goods_id` bigint NOT NULL COMMENT '所属商品ID',
  `sku_name` varchar(64) NOT NULL COMMENT 'SKU名称，如红色-XL',
  `spec_params` json NOT NULL COMMENT '规格参数，JSON格式',
  `price` decimal(10,2) NOT NULL COMMENT 'SKU销售价',
  `stock` int NOT NULL DEFAULT 0 COMMENT '库存数量',
  `lock_stock` int NOT NULL DEFAULT 0 COMMENT '锁定库存，已下单未支付占用库存',
  `stock_warn` int NOT NULL DEFAULT 10 COMMENT '库存预警值',
  `sku_code` varchar(32) NOT NULL COMMENT 'SKU编码，唯一',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT 'SKU状态：1-正常 0-下架',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`sku_id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_stock_status` (`stock`,`status`),
  KEY `idx_available_stock` (`status`,`is_deleted`,`stock`,`lock_stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品SKU与库存表';

CREATE TABLE `tb_goods_pic` (
  `pic_id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID，主键',
  `goods_id` bigint NOT NULL COMMENT '所属商品ID',
  `pic_url` varchar(255) NOT NULL COMMENT '图片地址',
  `pic_sort` int NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`pic_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品图片表';

CREATE TABLE `tb_goods_price_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '价格日志ID，主键',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `old_price` decimal(10,2) NOT NULL COMMENT '修改前价格',
  `new_price` decimal(10,2) NOT NULL COMMENT '修改后价格',
  `operator_id` bigint NOT NULL COMMENT '操作人ID，通常为商家ID',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品价格变更日志表';

CREATE TABLE `tb_sku_stock_log` (
  `stock_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '库存流水ID，主键',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `change_type` tinyint NOT NULL COMMENT '变动类型：1-商家调整 2-下单扣减 3-取消恢复 4-售后退货恢复 5-盘点调整',
  `change_num` int NOT NULL COMMENT '变动数量，增加为正，减少为负',
  `before_stock` int NOT NULL COMMENT '变动前库存',
  `after_stock` int NOT NULL COMMENT '变动后库存',
  `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `related_order_item_id` bigint DEFAULT NULL COMMENT '关联订单项ID',
  `operator_type` tinyint NOT NULL COMMENT '操作人类型：1-用户 2-商家 3-平台 4-系统',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`stock_log_id`),
  KEY `idx_sku_time` (`sku_id`,`create_time`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_related_order` (`related_order_id`),
  KEY `idx_change_type` (`change_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SKU库存流水表';

CREATE TABLE `tb_goods_comment` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '评价ID，主键',
  `order_item_id` bigint NOT NULL COMMENT '所属订单项ID，唯一',
  `user_id` bigint NOT NULL COMMENT '评价用户ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `goods_score` tinyint NOT NULL COMMENT '商品评分，1-5星',
  `service_score` tinyint NOT NULL COMMENT '服务评分，1-5星',
  `logistics_score` tinyint NOT NULL COMMENT '物流评分，1-5星',
  `comment_content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `comment_pic` text COMMENT '评价图片地址，逗号分隔，多图场景使用',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
  `is_valid` tinyint NOT NULL DEFAULT 1 COMMENT '是否有效：1-正常 0-违规删除',
  `is_anonymous` tinyint NOT NULL DEFAULT 0 COMMENT '是否匿名：0-否 1-是',
  `comment_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  `reply_content` varchar(200) DEFAULT NULL COMMENT '商家回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '商家回复时间',
  PRIMARY KEY (`comment_id`),
  UNIQUE KEY `uk_order_item_id` (`order_item_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评价表';

CREATE TABLE `tb_user_cart` (
  `cart_id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车项ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `buy_num` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `add_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `is_selected` tinyint NOT NULL DEFAULT 1 COMMENT '是否选中：0-否 1-是',
  PRIMARY KEY (`cart_id`),
  UNIQUE KEY `uk_user_sku` (`user_id`,`sku_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户购物车表';

CREATE TABLE `tb_order_group` (
  `group_id` bigint NOT NULL AUTO_INCREMENT COMMENT '父订单ID，主键，一次结算单',
  `group_no` varchar(32) NOT NULL COMMENT '父订单号，唯一，用于用户展示和统一支付',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '父订单商品总金额',
  `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '父订单总优惠金额',
  `freight_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '父订单总运费',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '父订单实际应付金额',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态：0-待支付 1-支付成功 2-支付失败 3-已关闭 4-退款中 5-已退款',
  `group_status` tinyint NOT NULL DEFAULT 0 COMMENT '父订单状态：0-待支付 1-待履约 2-部分发货 3-已完成 4-已取消 5-售后中',
  `order_count` int NOT NULL DEFAULT 1 COMMENT '包含子订单数量',
  `pay_type` tinyint DEFAULT NULL COMMENT '支付方式：1-微信 2-支付宝 3-银行卡 9-模拟支付',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `expire_time` datetime NOT NULL COMMENT '支付过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`group_id`),
  UNIQUE KEY `uk_group_no` (`group_no`),
  KEY `idx_user_status` (`user_id`,`group_status`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='父订单表，一次结算单';

CREATE TABLE `tb_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '子订单ID，主键',
  `order_no` varchar(32) NOT NULL COMMENT '子订单号，唯一，用于展示、发货、售后',
  `group_id` bigint NOT NULL COMMENT '父订单ID',
  `group_no` varchar(32) NOT NULL COMMENT '父订单号，冗余便于查询',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `merchant_id` bigint NOT NULL COMMENT '所属商家ID，一个子订单只对应一个商家',
  `total_amount` decimal(10,2) NOT NULL COMMENT '子订单商品总金额',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '子订单分摊后实际支付金额',
  `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '子订单优惠抵扣金额',
  `freight` decimal(6,2) NOT NULL DEFAULT 0.00 COMMENT '子订单运费',
  `pay_type` tinyint DEFAULT NULL COMMENT '支付方式：1-微信 2-支付宝 3-银行卡 9-模拟支付',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态：0-待支付 1-支付成功 2-支付失败 3-已关闭 4-退款中 5-已退款',
  `order_status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付 1-待发货 2-待收货 3-已完成 4-已取消 5-售后中',
  `addr_id` bigint NOT NULL COMMENT '收货地址ID',
  `consignee` varchar(20) NOT NULL COMMENT '收货人快照',
  `consignee_phone` varchar(11) NOT NULL COMMENT '收货电话快照',
  `receive_addr` varchar(255) NOT NULL COMMENT '收货地址快照',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '确认收货时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消订单时间',
  `expire_time` datetime NOT NULL COMMENT '支付过期时间',
  `buyer_remark` varchar(200) DEFAULT NULL COMMENT '买家留言',
  `merchant_remark` varchar(200) DEFAULT NULL COMMENT '商家备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='子订单表，每个商家一个子订单';

CREATE TABLE `tb_order_item` (
  `order_item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单项ID，主键',
  `order_id` bigint NOT NULL COMMENT '所属子订单ID',
  `group_id` bigint NOT NULL COMMENT '所属父订单ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID，冗余便于商家查询',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `goods_name` varchar(128) NOT NULL COMMENT '商品名称快照',
  `sku_name` varchar(64) NOT NULL COMMENT 'SKU名称快照',
  `goods_pic` varchar(255) NOT NULL COMMENT '商品主图快照',
  `price` decimal(10,2) NOT NULL COMMENT '成交单价',
  `num` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_price` decimal(10,2) NOT NULL COMMENT '订单项小计',
  `comment_status` tinyint NOT NULL DEFAULT 0 COMMENT '评价状态：0-未评价 1-已评价',
  `after_sale_status` tinyint NOT NULL DEFAULT 0 COMMENT '售后状态：0-无售后 1-售后中 2-售后完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`order_item_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_sku_id` (`sku_id`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';

CREATE TABLE `tb_order_status_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单状态日志ID，主键',
  `target_type` tinyint NOT NULL COMMENT '对象类型：1-父订单 2-子订单',
  `group_id` bigint DEFAULT NULL COMMENT '父订单ID',
  `order_id` bigint DEFAULT NULL COMMENT '子订单ID',
  `before_status` tinyint DEFAULT NULL COMMENT '变更前订单状态',
  `after_status` tinyint NOT NULL COMMENT '变更后订单状态',
  `before_pay_status` tinyint DEFAULT NULL COMMENT '变更前支付状态',
  `after_pay_status` tinyint DEFAULT NULL COMMENT '变更后支付状态',
  `operator_type` tinyint NOT NULL COMMENT '操作人类型：1-用户 2-商家 3-平台管理员 4-系统',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operation_desc` varchar(200) NOT NULL COMMENT '操作描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_target_time` (`target_type`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单状态流转日志表';

CREATE TABLE `tb_payment` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付单ID，主键',
  `pay_no` varchar(32) NOT NULL COMMENT '支付单号，唯一',
  `group_id` bigint NOT NULL COMMENT '父订单ID，一次支付对应一个父订单',
  `order_id` bigint DEFAULT NULL COMMENT '子订单ID，单商家支付时可填',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `pay_channel` tinyint NOT NULL COMMENT '支付渠道：1-微信 2-支付宝 3-银行卡 9-模拟支付',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态：0-待支付 1-成功 2-失败 3-关闭 4-退款中 5-已退款',
  `third_trade_no` varchar(64) DEFAULT NULL COMMENT '第三方交易号',
  `pay_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `expire_time` datetime NOT NULL COMMENT '支付过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_pay_no` (`pay_no`),
  UNIQUE KEY `uk_group_id` (`group_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_status` (`user_id`,`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付单表';

CREATE TABLE `tb_refund` (
  `refund_id` bigint NOT NULL AUTO_INCREMENT COMMENT '退款单ID，主键',
  `refund_no` varchar(32) NOT NULL COMMENT '退款单号，唯一',
  `payment_id` bigint NOT NULL COMMENT '支付单ID',
  `group_id` bigint NOT NULL COMMENT '父订单ID',
  `order_id` bigint NOT NULL COMMENT '子订单ID',
  `after_sale_id` bigint DEFAULT NULL COMMENT '售后单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `refund_amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `refund_status` tinyint NOT NULL DEFAULT 0 COMMENT '退款状态：0-待退款 1-退款中 2-成功 3-失败',
  `refund_channel` tinyint NOT NULL COMMENT '退款渠道：1-微信 2-支付宝 3-银行卡 9-模拟退款',
  `third_refund_no` varchar(64) DEFAULT NULL COMMENT '第三方退款单号',
  `reason` varchar(200) DEFAULT NULL COMMENT '退款原因',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `success_time` datetime DEFAULT NULL COMMENT '退款成功时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`refund_id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_payment_id` (`payment_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_after_sale_id` (`after_sale_id`),
  KEY `idx_user_status` (`user_id`,`refund_status`),
  KEY `idx_merchant_status` (`merchant_id`,`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款单表';

CREATE TABLE `tb_logistics` (
  `logistics_id` bigint NOT NULL AUTO_INCREMENT COMMENT '物流ID，主键',
  `order_id` bigint NOT NULL COMMENT '所属子订单ID',
  `merchant_id` bigint NOT NULL COMMENT '所属商家ID',
  `biz_type` tinyint NOT NULL DEFAULT 0 COMMENT '业务类型：0-订单发货 1-售后买家寄回 2-售后商家发货',
  `express_company` varchar(32) DEFAULT NULL COMMENT '快递公司名称',
  `express_no` varchar(32) DEFAULT NULL COMMENT '快递单号',
  `logistics_status` tinyint NOT NULL DEFAULT 0 COMMENT '物流状态：0-待发货 1-待揽收 2-运输中 3-派送中 4-已签收 5-拒收',
  `predict_receive_time` datetime DEFAULT NULL COMMENT '预计到货时间',
  `sign_time` datetime DEFAULT NULL COMMENT '实际签收时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`logistics_id`),
  UNIQUE KEY `uk_order_biz` (`order_id`,`biz_type`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_logistics_status` (`logistics_status`),
  KEY `idx_express_no` (`express_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单物流主表';

CREATE TABLE `tb_logistics_trace` (
  `trace_id` bigint NOT NULL AUTO_INCREMENT COMMENT '物流轨迹ID，主键',
  `logistics_id` bigint NOT NULL COMMENT '所属物流ID',
  `trace_content` varchar(255) NOT NULL COMMENT '轨迹描述',
  `trace_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '轨迹时间',
  `trace_location` varchar(64) DEFAULT NULL COMMENT '轨迹地点',
  PRIMARY KEY (`trace_id`),
  KEY `idx_logistics_id` (`logistics_id`),
  KEY `idx_trace_time` (`trace_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物流轨迹表';

CREATE TABLE `tb_after_sale` (
  `after_sale_id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后单ID，主键',
  `after_sale_no` varchar(32) NOT NULL COMMENT '售后单号，唯一',
  `group_id` bigint NOT NULL COMMENT '父订单ID',
  `order_id` bigint NOT NULL COMMENT '子订单ID',
  `order_item_id` bigint NOT NULL COMMENT '订单项ID',
  `user_id` bigint NOT NULL COMMENT '申请用户ID',
  `merchant_id` bigint NOT NULL COMMENT '处理商家ID',
  `after_sale_type` tinyint NOT NULL COMMENT '售后类型：1-退款 2-换货 3-补发 4-退货退款',
  `apply_reason` varchar(200) NOT NULL COMMENT '申请原因',
  `apply_evidence` text COMMENT '凭证地址，逗号分隔，多图场景使用',
  `apply_amount` decimal(10,2) NOT NULL COMMENT '申请金额',
  `handle_status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态：0-待商家处理 1-商家同意 2-商家拒绝 3-平台介入 4-处理完成 5-已撤销',
  `merchant_remark` varchar(200) DEFAULT NULL COMMENT '商家处理意见',
  `platform_remark` varchar(200) DEFAULT NULL COMMENT '平台介入意见',
  `handle_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`after_sale_id`),
  UNIQUE KEY `uk_after_sale_no` (`after_sale_no`),
  UNIQUE KEY `uk_after_sale_order_id` (`order_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_item_id` (`order_item_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_handle_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后申请表';

CREATE TABLE `tb_after_sale_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后日志ID，主键',
  `after_sale_id` bigint NOT NULL COMMENT '售后单ID',
  `before_status` tinyint DEFAULT NULL COMMENT '变更前状态',
  `after_status` tinyint NOT NULL COMMENT '变更后状态',
  `operator_type` tinyint NOT NULL COMMENT '操作人类型：1-用户 2-商家 3-平台管理员 4-系统',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operation_desc` varchar(200) NOT NULL COMMENT '操作描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_after_sale_id` (`after_sale_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后状态日志表';

CREATE TABLE `tb_after_sale_logistics` (
  `asl_id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后物流ID，主键',
  `after_sale_id` bigint NOT NULL COMMENT '售后单ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `express_company` varchar(32) NOT NULL COMMENT '退货快递公司',
  `express_no` varchar(32) NOT NULL COMMENT '退货快递单号',
  `send_time` datetime DEFAULT NULL COMMENT '用户寄出时间',
  `receive_time` datetime DEFAULT NULL COMMENT '商家签收时间',
  `logistics_status` tinyint NOT NULL DEFAULT 0 COMMENT '退货物流状态：0-待寄回 1-运输中 2-已签收 3-异常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`asl_id`),
  KEY `idx_after_sale_id` (`after_sale_id`),
  KEY `idx_express_no` (`express_no`),
  KEY `idx_merchant_status` (`merchant_id`,`logistics_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后退货物流表';

CREATE TABLE `tb_dispute` (
  `dispute_id` bigint NOT NULL AUTO_INCREMENT COMMENT '纠纷仲裁ID，主键',
  `dispute_no` varchar(32) NOT NULL COMMENT '纠纷单号，唯一',
  `after_sale_id` bigint NOT NULL COMMENT '关联售后单ID',
  `group_id` bigint NOT NULL COMMENT '父订单ID',
  `order_id` bigint NOT NULL COMMENT '子订单ID',
  `order_item_id` bigint DEFAULT NULL COMMENT '订单项ID，可为空',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `apply_type` tinyint NOT NULL COMMENT '发起方：1-用户 2-商家 3-平台',
  `dispute_reason` varchar(200) NOT NULL COMMENT '纠纷原因',
  `dispute_desc` varchar(1000) DEFAULT NULL COMMENT '纠纷详细说明',
  `dispute_status` tinyint NOT NULL DEFAULT 0 COMMENT '纠纷状态：0-待平台处理 1-举证中 2-平台处理中 3-已裁决 4-已关闭',
  `judge_result` tinyint DEFAULT NULL COMMENT '判责结果：1-支持用户 2-支持商家 3-部分支持 4-双方协商关闭',
  `final_amount` decimal(10,2) DEFAULT NULL COMMENT '最终退款或补偿金额',
  `platform_admin_id` bigint DEFAULT NULL COMMENT '处理平台管理员ID',
  `platform_opinion` varchar(500) DEFAULT NULL COMMENT '平台处理意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `judge_time` datetime DEFAULT NULL COMMENT '裁决时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dispute_id`),
  UNIQUE KEY `uk_dispute_no` (`dispute_no`),
  UNIQUE KEY `uk_after_sale_id` (`after_sale_id`),
  KEY `idx_user_status` (`user_id`,`dispute_status`),
  KEY `idx_merchant_status` (`merchant_id`,`dispute_status`),
  KEY `idx_admin_status` (`platform_admin_id`,`dispute_status`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台纠纷仲裁表';

CREATE TABLE `tb_dispute_evidence` (
  `evidence_id` bigint NOT NULL AUTO_INCREMENT COMMENT '纠纷证据ID，主键',
  `dispute_id` bigint NOT NULL COMMENT '纠纷仲裁ID',
  `submitter_type` tinyint NOT NULL COMMENT '提交方类型：1-用户 2-商家 3-平台管理员',
  `submitter_id` bigint NOT NULL COMMENT '提交方ID',
  `evidence_type` tinyint NOT NULL DEFAULT 1 COMMENT '证据类型：1-图片 2-聊天记录 3-订单截图 4-物流截图 5-文字说明 6-其他',
  `evidence_content` text NOT NULL COMMENT '证据内容，图片URL、截图URL或文字说明',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `is_valid` tinyint NOT NULL DEFAULT 1 COMMENT '是否有效：1-有效 0-无效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`evidence_id`),
  KEY `idx_dispute_id` (`dispute_id`),
  KEY `idx_submitter` (`submitter_type`,`submitter_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='纠纷举证表';

CREATE TABLE `tb_coupon` (
  `coupon_id` bigint NOT NULL AUTO_INCREMENT COMMENT '优惠券ID，主键',
  `coupon_type` tinyint NOT NULL COMMENT '优惠券类型：1-平台券 2-商家券',
  `merchant_id` bigint DEFAULT NULL COMMENT '所属商家ID，平台券为空',
  `coupon_name` varchar(64) NOT NULL COMMENT '优惠券名称',
  `discount_type` tinyint NOT NULL DEFAULT 1 COMMENT '优惠类型：1-满减券 2-折扣券 3-无门槛券',
  `denomination` decimal(8,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券面额，满减券使用',
  `discount_rate` decimal(5,4) DEFAULT NULL COMMENT '折扣率，折扣券使用，如0.8000表示8折',
  `min_amount` decimal(8,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
  `per_limit` int NOT NULL DEFAULT 1 COMMENT '每人限领张数',
  `limit_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否限领：1-限领 0-不限领',
  `can_stack` tinyint NOT NULL DEFAULT 0 COMMENT '是否可叠加：0-否 1-是',
  `start_time` datetime NOT NULL COMMENT '生效时间',
  `end_time` datetime NOT NULL COMMENT '过期时间',
  `total_num` int NOT NULL COMMENT '总发行量',
  `surplus_num` int NOT NULL COMMENT '剩余数量',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核 1-通过 2-驳回',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-未上线 1-已上线 2-已下线',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`coupon_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_coupon_type` (`coupon_type`),
  KEY `idx_status_time` (`status`,`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券定义表';

CREATE TABLE `tb_coupon_scope` (
  `scope_id` bigint NOT NULL AUTO_INCREMENT COMMENT '适用范围ID，主键',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
  `scope_type` tinyint NOT NULL COMMENT '适用范围类型：1-全场 2-指定商家 3-指定分类 4-指定商品',
  `target_id` bigint NOT NULL DEFAULT 0 COMMENT '范围目标ID，全场为0，商家/分类/商品则填对应ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`scope_id`),
  UNIQUE KEY `uk_coupon_scope` (`coupon_id`,`scope_type`,`target_id`),
  KEY `idx_scope_query` (`scope_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券适用范围表';

CREATE TABLE `tb_user_coupon` (
  `user_coupon_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户优惠券ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
  `merchant_id` bigint DEFAULT NULL COMMENT '所属商家ID，平台券为空',
  `use_status` tinyint NOT NULL DEFAULT 0 COMMENT '使用状态：0-未使用 1-已使用 2-已过期 3-已锁定',
  `lock_group_id` bigint DEFAULT NULL COMMENT '下单锁定的父订单ID',
  `use_group_id` bigint DEFAULT NULL COMMENT '实际使用的父订单ID',
  `use_order_id` bigint DEFAULT NULL COMMENT '实际使用的子订单ID，可为空',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `receive_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `expire_time` datetime NOT NULL COMMENT '过期时间，冗余自优惠券定义',
  PRIMARY KEY (`user_coupon_id`),
  KEY `idx_user_status` (`user_id`,`use_status`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_lock_group_id` (`lock_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户领券记录表';

CREATE TABLE `tb_order_coupon` (
  `order_coupon_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单优惠券记录ID，主键',
  `group_id` bigint NOT NULL COMMENT '父订单ID',
  `order_id` bigint DEFAULT NULL COMMENT '子订单ID，平台券按父订单使用时可为空',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
  `user_coupon_id` bigint NOT NULL COMMENT '用户优惠券ID',
  `coupon_name` varchar(64) NOT NULL COMMENT '优惠券名称快照',
  `discount_amount` decimal(10,2) NOT NULL COMMENT '本次实际优惠金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`order_coupon_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_coupon_id` (`user_coupon_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单使用优惠券记录表';

CREATE TABLE `tb_user_collect` (
  `collect_id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `collect_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `is_cancel` tinyint NOT NULL DEFAULT 0 COMMENT '是否取消：0-未取消 1-已取消',
  PRIMARY KEY (`collect_id`),
  UNIQUE KEY `uk_user_goods` (`user_id`,`goods_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户商品收藏表';

CREATE TABLE `tb_shop_follow` (
  `follow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '店铺关注ID，主键',
  `user_id` bigint NOT NULL COMMENT '买家用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`follow_id`),
  UNIQUE KEY `uk_user_merchant` (`user_id`,`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺关注表';

CREATE TABLE `tb_user_browse_history` (
  `history_id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint DEFAULT NULL COMMENT '最后浏览SKU ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `source_type` tinyint DEFAULT NULL COMMENT '来源：1-首页 2-分类 3-搜索 4-推荐 5-直播',
  `browse_count` int NOT NULL DEFAULT 1 COMMENT '浏览次数',
  `last_browse_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后浏览时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`history_id`),
  UNIQUE KEY `uk_user_goods` (`user_id`,`goods_id`),
  KEY `idx_user_time` (`user_id`,`last_browse_time`),
  KEY `idx_goods_time` (`goods_id`,`last_browse_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户浏览记录表';

CREATE TABLE `tb_search_history` (
  `search_id` bigint NOT NULL AUTO_INCREMENT COMMENT '搜索记录ID，主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID，未登录可为空',
  `keyword` varchar(100) NOT NULL COMMENT '搜索关键词',
  `result_count` int NOT NULL DEFAULT 0 COMMENT '搜索结果数量',
  `search_source` tinyint NOT NULL DEFAULT 1 COMMENT '来源：1-普通搜索 2-分类内搜索 3-直播间搜索',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`search_id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_keyword_time` (`keyword`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索历史表';

CREATE TABLE `tb_hot_search_keyword` (
  `hot_id` bigint NOT NULL AUTO_INCREMENT COMMENT '热搜词ID，主键',
  `keyword` varchar(100) NOT NULL COMMENT '热搜关键词',
  `search_count` int NOT NULL DEFAULT 0 COMMENT '搜索次数',
  `hot_score` decimal(12,4) NOT NULL DEFAULT 0.0000 COMMENT '热度分数',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-展示 0-隐藏',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`hot_id`),
  UNIQUE KEY `uk_keyword_date` (`keyword`,`stat_date`),
  KEY `idx_date_score` (`stat_date`,`hot_score`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='热搜关键词表';

CREATE TABLE `tb_recommend_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '推荐记录ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `scene` tinyint NOT NULL COMMENT '推荐场景：1-首页 2-详情页 3-购物车 4-订单页 5-直播间',
  `reason` varchar(100) DEFAULT NULL COMMENT '推荐原因',
  `score` decimal(10,4) DEFAULT NULL COMMENT '推荐分数',
  `is_click` tinyint NOT NULL DEFAULT 0 COMMENT '是否点击：0-否 1-是',
  `is_buy` tinyint NOT NULL DEFAULT 0 COMMENT '是否购买：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_user_scene_time` (`user_id`,`scene`,`create_time`),
  KEY `idx_goods_time` (`goods_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='个性化推荐记录表';

CREATE TABLE `tb_goods_rank_snapshot` (
  `rank_id` bigint NOT NULL AUTO_INCREMENT COMMENT '榜单记录ID，主键',
  `rank_type` tinyint NOT NULL COMMENT '榜单类型：1-销量榜 2-热搜榜 3-好评榜 4-新品榜 5-直播榜',
  `cate_id` int NOT NULL DEFAULT 0 COMMENT '分类ID，0表示全站榜单',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `rank_no` int NOT NULL COMMENT '排名',
  `rank_score` decimal(12,4) DEFAULT NULL COMMENT '榜单分数',
  `snapshot_date` date NOT NULL COMMENT '快照日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`rank_id`),
  UNIQUE KEY `uk_rank_goods_date` (`rank_type`,`cate_id`,`goods_id`,`snapshot_date`),
  KEY `idx_rank_query` (`rank_type`,`cate_id`,`snapshot_date`,`rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品榜单快照表';

CREATE TABLE `tb_live` (
  `live_id` bigint NOT NULL AUTO_INCREMENT COMMENT '直播ID，主键',
  `merchant_id` bigint NOT NULL COMMENT '开播商家ID',
  `live_title` varchar(64) NOT NULL COMMENT '直播标题',
  `live_cover` varchar(255) NOT NULL COMMENT '直播封面',
  `live_theme` varchar(100) NOT NULL COMMENT '直播主题',
  `live_url` varchar(255) DEFAULT NULL COMMENT '真实直播链接',
  `start_time` datetime NOT NULL COMMENT '开播时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `live_status` tinyint NOT NULL DEFAULT 0 COMMENT '直播状态：0-未开播 1-直播中 2-已结束 3-违规中断',
  `watch_num` bigint NOT NULL DEFAULT 0 COMMENT '观看人数',
  `interact_num` int NOT NULL DEFAULT 0 COMMENT '互动数，点赞和评论等',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`live_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_live_status` (`live_status`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='直播间表';

CREATE TABLE `tb_live_goods` (
  `lg_id` bigint NOT NULL AUTO_INCREMENT COMMENT '直播商品关联ID，主键',
  `live_id` bigint NOT NULL COMMENT '直播ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `live_price` decimal(10,2) NOT NULL COMMENT '直播间专属价',
  `goods_sort` int NOT NULL DEFAULT 0 COMMENT '商品排序',
  `is_on_shelf` tinyint NOT NULL DEFAULT 1 COMMENT '是否上架直播间：1-是 0-否',
  PRIMARY KEY (`lg_id`),
  KEY `idx_live_id` (`live_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='直播间商品关联表';

CREATE TABLE `tb_live_comment` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '直播评论ID，主键',
  `live_id` bigint NOT NULL COMMENT '直播ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` varchar(300) NOT NULL COMMENT '评论内容',
  `comment_type` tinyint NOT NULL DEFAULT 1 COMMENT '评论类型：1-普通评论 2-提问 3-点赞提示',
  `is_valid` tinyint NOT NULL DEFAULT 1 COMMENT '是否有效：1-正常 0-删除/违规',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`comment_id`),
  KEY `idx_live_time` (`live_id`,`create_time`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='直播评论表';

CREATE TABLE `tb_live_like` (
  `like_id` bigint NOT NULL AUTO_INCREMENT COMMENT '直播点赞ID，主键',
  `live_id` bigint NOT NULL COMMENT '直播ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `like_status` tinyint NOT NULL DEFAULT 1 COMMENT '点赞状态：1-已点赞 0-已取消',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`like_id`),
  UNIQUE KEY `uk_live_user` (`live_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='直播点赞记录表';

CREATE TABLE `tb_live_watch_record` (
  `watch_id` bigint NOT NULL AUTO_INCREMENT COMMENT '观看记录ID，主键',
  `live_id` bigint NOT NULL COMMENT '直播ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `enter_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '进入直播间时间',
  `leave_time` datetime DEFAULT NULL COMMENT '离开直播间时间',
  `watch_seconds` int NOT NULL DEFAULT 0 COMMENT '观看时长，单位秒',
  PRIMARY KEY (`watch_id`),
  KEY `idx_live_user` (`live_id`,`user_id`),
  KEY `idx_user_time` (`user_id`,`enter_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='直播观看记录表';

CREATE TABLE `tb_banner` (
  `banner_id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮播图ID，主键',
  `banner_title` varchar(64) NOT NULL COMMENT '轮播图标题',
  `image_url` varchar(255) NOT NULL COMMENT '图片地址',
  `jump_type` tinyint NOT NULL DEFAULT 0 COMMENT '跳转类型：0-不跳转 1-商品 2-分类 3-活动 4-直播 5-外链',
  `jump_value` varchar(255) DEFAULT NULL COMMENT '跳转目标值，如ID或URL',
  `display_position` tinyint NOT NULL DEFAULT 1 COMMENT '展示位置：1-首页 2-分类页 3-直播页',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-展示 0-隐藏',
  `start_time` datetime DEFAULT NULL COMMENT '开始展示时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束展示时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`banner_id`),
  KEY `idx_position_status` (`display_position`,`status`,`sort_no`),
  KEY `idx_time` (`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页轮播和运营位表';

CREATE TABLE `tb_home_section` (
  `section_id` bigint NOT NULL AUTO_INCREMENT COMMENT '首页楼层ID，主键',
  `section_name` varchar(64) NOT NULL COMMENT '楼层名称，如热门推荐、数码专区',
  `section_type` tinyint NOT NULL COMMENT '楼层类型：1-商品楼层 2-分类入口 3-活动楼层 4-直播楼层',
  `section_sort` int NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-展示 0-隐藏',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`section_id`),
  KEY `idx_sort_status` (`section_sort`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页楼层表';

CREATE TABLE `tb_home_section_goods` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '楼层商品ID，主键',
  `section_id` bigint NOT NULL COMMENT '首页楼层ID',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-展示 0-隐藏',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_section_goods` (`section_id`,`goods_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_section_sort` (`section_id`,`sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页楼层商品关联表';

CREATE TABLE `tb_platform_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID，主键',
  `notice_title` varchar(128) NOT NULL COMMENT '公告标题',
  `notice_content` text NOT NULL COMMENT '公告内容',
  `notice_type` tinyint NOT NULL COMMENT '公告类型：1-系统维护 2-活动通知 3-规则更新 4-违规处罚',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-已发布 0-草稿 2-已下架',
  `create_admin_id` bigint NOT NULL COMMENT '发布管理员ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台公告表';

CREATE TABLE `tb_platform_activity` (
  `activity_id` bigint NOT NULL AUTO_INCREMENT COMMENT '活动ID，主键',
  `activity_name` varchar(64) NOT NULL COMMENT '活动名称',
  `activity_type` tinyint NOT NULL COMMENT '活动类型：1-满减 2-秒杀 3-拼团 4-限时折扣',
  `start_time` datetime NOT NULL COMMENT '活动开始时间',
  `end_time` datetime NOT NULL COMMENT '活动结束时间',
  `activity_rule` text NOT NULL COMMENT '活动规则，JSON格式',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '活动状态：0-未上线 1-已上线 2-已结束 3-已下架',
  `create_admin_id` bigint NOT NULL COMMENT '创建管理员ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`activity_id`),
  KEY `idx_activity_type` (`activity_type`),
  KEY `idx_status` (`status`),
  KEY `idx_time` (`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台活动表';

CREATE TABLE `tb_merchant_activity` (
  `ma_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商家活动报名ID，主键',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `goods_id` bigint NOT NULL COMMENT '报名商品ID',
  `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核 1-通过 2-驳回',
  `audit_remark` varchar(200) DEFAULT NULL COMMENT '审核意见',
  `sign_up_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`ma_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家活动报名表';

CREATE TABLE `tb_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID，主键',
  `receiver_type` tinyint NOT NULL COMMENT '接收方类型：1-用户 2-商家 3-平台管理员',
  `receiver_id` bigint NOT NULL COMMENT '接收方ID',
  `message_type` tinyint NOT NULL COMMENT '消息类型：1-订单 2-售后 3-优惠券 4-直播 5-审核 6-系统公告 7-违规处罚',
  `title` varchar(100) NOT NULL COMMENT '消息标题',
  `content` varchar(500) NOT NULL COMMENT '消息内容',
  `related_type` tinyint DEFAULT NULL COMMENT '关联对象类型：1-订单 2-售后 3-商品 4-优惠券 5-直播 6-商家',
  `related_id` bigint DEFAULT NULL COMMENT '关联对象ID',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_receiver_read` (`receiver_type`,`receiver_id`,`is_read`),
  KEY `idx_message_type` (`message_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='三端消息通知表';

CREATE TABLE `tb_chat_session` (
  `session_id` bigint NOT NULL AUTO_INCREMENT COMMENT '客服会话ID，主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `session_status` tinyint NOT NULL DEFAULT 1 COMMENT '会话状态：1-进行中 2-已结束 3-已拉黑',
  `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_content` varchar(500) DEFAULT NULL COMMENT '最后一条消息摘要',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `user_unread_count` int NOT NULL DEFAULT 0 COMMENT '用户未读数',
  `merchant_unread_count` int NOT NULL DEFAULT 0 COMMENT '商家未读数',
  `ai_paused` tinyint NOT NULL DEFAULT 0 COMMENT 'AI是否暂停回复：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `uk_user_merchant` (`user_id`,`merchant_id`),
  KEY `idx_merchant_time` (`merchant_id`,`last_message_time`),
  KEY `idx_user_time` (`user_id`,`last_message_time`),
  KEY `idx_session_status` (`session_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服会话表';

CREATE TABLE `tb_chat_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '聊天消息ID，主键',
  `session_id` bigint NOT NULL COMMENT '所属会话ID',
  `sender_type` tinyint NOT NULL COMMENT '发送方类型：1-用户 2-商家客服 3-AI自动回复 4-系统',
  `sender_id` bigint DEFAULT NULL COMMENT '发送方ID，AI或系统可为空',
  `receiver_type` tinyint NOT NULL COMMENT '接收方类型：1-用户 2-商家',
  `receiver_id` bigint NOT NULL COMMENT '接收方ID',
  `message_type` tinyint NOT NULL DEFAULT 1 COMMENT '消息类型：1-文字 2-图片 3-商品卡片 4-订单卡片 5-系统提示',
  `content` text NOT NULL COMMENT '消息内容，文字、图片URL或卡片JSON',
  `related_type` tinyint DEFAULT NULL COMMENT '关联对象类型：1-商品 2-订单 3-售后 4-优惠券',
  `related_id` bigint DEFAULT NULL COMMENT '关联对象ID',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_session_time` (`session_id`,`create_time`),
  KEY `idx_receiver_read` (`receiver_type`,`receiver_id`,`is_read`),
  KEY `idx_related` (`related_type`,`related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服聊天消息表';

CREATE TABLE `tb_merchant_push_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商家推送任务ID，主键',
  `task_no` varchar(32) NOT NULL COMMENT '推送任务编号，唯一',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `task_title` varchar(100) NOT NULL COMMENT '推送标题',
  `task_content` varchar(1000) NOT NULL COMMENT '推送内容',
  `target_type` tinyint NOT NULL COMMENT '目标用户范围：1-全部购买用户 2-收藏店铺用户 3-指定用户 4-最近浏览用户',
  `target_condition` text DEFAULT NULL COMMENT '目标用户条件JSON，如指定用户ID列表或筛选条件',
  `related_type` tinyint DEFAULT NULL COMMENT '关联对象类型：1-商品 2-优惠券 3-直播 4-活动',
  `related_id` bigint DEFAULT NULL COMMENT '关联对象ID',
  `task_status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态：0-草稿 1-待发送 2-发送中 3-发送完成 4-发送失败 5-已取消',
  `plan_send_time` datetime DEFAULT NULL COMMENT '计划发送时间',
  `actual_send_time` datetime DEFAULT NULL COMMENT '实际发送时间',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功发送数量',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '发送失败数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_merchant_status` (`merchant_id`,`task_status`),
  KEY `idx_plan_send_time` (`plan_send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家消息群发任务表';

CREATE TABLE `tb_platform_daily_stat` (
  `stat_id` bigint NOT NULL AUTO_INCREMENT COMMENT '平台统计ID，主键',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `new_user_count` int NOT NULL DEFAULT 0 COMMENT '新增用户数',
  `new_merchant_count` int NOT NULL DEFAULT 0 COMMENT '新增商家数',
  `order_count` int NOT NULL DEFAULT 0 COMMENT '下单数',
  `pay_order_count` int NOT NULL DEFAULT 0 COMMENT '支付订单数',
  `gross_sales` decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '商品交易总额GMV',
  `pay_amount` decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `refund_amount` decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`stat_id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台每日统计表';

CREATE TABLE `tb_merchant_daily_stat` (
  `stat_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商家统计ID，主键',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `goods_view_count` int NOT NULL DEFAULT 0 COMMENT '商品浏览量',
  `order_count` int NOT NULL DEFAULT 0 COMMENT '订单数',
  `pay_order_count` int NOT NULL DEFAULT 0 COMMENT '支付订单数',
  `sales_amount` decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '销售金额',
  `refund_amount` decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  `new_comment_count` int NOT NULL DEFAULT 0 COMMENT '新增评价数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`stat_id`),
  UNIQUE KEY `uk_merchant_date` (`merchant_id`,`stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家每日统计表';

CREATE TABLE `tb_goods_daily_stat` (
  `stat_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品统计ID，主键',
  `goods_id` bigint NOT NULL COMMENT '商品ID',
  `merchant_id` bigint NOT NULL COMMENT '商家ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `collect_count` int NOT NULL DEFAULT 0 COMMENT '收藏数',
  `cart_count` int NOT NULL DEFAULT 0 COMMENT '加购数',
  `order_count` int NOT NULL DEFAULT 0 COMMENT '下单件数',
  `pay_count` int NOT NULL DEFAULT 0 COMMENT '支付件数',
  `sales_amount` decimal(14,2) NOT NULL DEFAULT 0.00 COMMENT '销售金额',
  `comment_count` int NOT NULL DEFAULT 0 COMMENT '评价数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`stat_id`),
  UNIQUE KEY `uk_goods_date` (`goods_id`,`stat_date`),
  KEY `idx_merchant_date` (`merchant_id`,`stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品每日统计表';

CREATE TABLE `tb_abnormal_order` (
  `abnormal_id` bigint NOT NULL AUTO_INCREMENT COMMENT '异常ID，主键',
  `order_id` bigint NOT NULL COMMENT '异常子订单ID，唯一',
  `abnormal_type` tinyint NOT NULL COMMENT '异常类型：1-刷单 2-恶意退款 3-盗卡支付 4-虚假下单',
  `abnormal_desc` varchar(200) NOT NULL COMMENT '异常描述',
  `handle_status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态：0-待审核 1-确认异常 2-误判解除',
  `handle_admin_id` bigint DEFAULT NULL COMMENT '处理管理员ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`abnormal_id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_abnormal_type` (`abnormal_type`),
  KEY `idx_handle_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异常交易记录表';

CREATE TABLE `tb_violation` (
  `violation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '违规ID，主键',
  `violation_type` tinyint NOT NULL COMMENT '违规主体：1-用户 2-商家 3-商品',
  `target_id` bigint NOT NULL COMMENT '主体ID，用户ID/商家ID/商品ID',
  `violation_content` varchar(500) NOT NULL COMMENT '违规内容',
  `violation_grade` tinyint NOT NULL COMMENT '违规等级：1-轻微 2-一般 3-严重',
  `punish_type` tinyint NOT NULL COMMENT '处罚类型：1-警告 2-限流 3-下架 4-限制功能 5-封号/封店',
  `punish_days` int DEFAULT NULL COMMENT '处罚时长，单位天，永久为空',
  `handle_admin_id` bigint NOT NULL COMMENT '处理管理员ID',
  `handle_remark` varchar(200) DEFAULT NULL COMMENT '处理意见',
  `handle_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态：0-已处理 1-整改中 2-已解封',
  `unlock_time` datetime DEFAULT NULL COMMENT '解封时间',
  PRIMARY KEY (`violation_id`),
  KEY `idx_violation_type` (`violation_type`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_handle_admin_id` (`handle_admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='违规处理记录表';

CREATE TABLE `tb_operation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '操作日志ID，主键',
  `operator_type` tinyint NOT NULL COMMENT '操作人类型：1-用户 2-商家 3-平台管理员 4-系统',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operation_module` varchar(32) NOT NULL COMMENT '操作模块，如商品、订单、售后',
  `operation_content` varchar(200) NOT NULL COMMENT '操作内容',
  `operation_ip` varchar(45) NOT NULL COMMENT '操作IP，兼容IPv4和IPv6',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `operation_result` tinyint NOT NULL DEFAULT 1 COMMENT '操作结果：1-成功 0-失败',
  PRIMARY KEY (`log_id`),
  KEY `idx_operator_type` (`operator_type`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operation_module` (`operation_module`),
  KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台操作日志表';

-- =========================================================
-- 演示测试数据
-- 说明：以下数据覆盖用户端、商家端、平台端、父子订单、支付、物流、售后、优惠券、直播、客服、纠纷与统计看板。
-- 密码字段使用 BCrypt 示例哈希：明文可约定为 123456（仅供本地演示，不用于真实生产）。
-- =========================================================

INSERT INTO `tb_user` (`user_id`,`phone`,`password`,`nickname`,`avatar`,`real_name`,`auth_status`,`register_time`,`last_login_time`,`login_ip`,`status`,`is_deleted`) VALUES
(1001,'13800000001','$2a$10$7EqJtq98hPqEX7fNZaFWoOhi6WxPvq6WqZQ3Jk8hH2I1QWvVV7Y8K','小明','/default/avatar.png','张小明',1,'2026-04-01 10:00:00','2026-05-05 09:00:00','127.0.0.1',1,0),
(1002,'13800000002','$2a$10$7EqJtq98hPqEX7fNZaFWoOhi6WxPvq6WqZQ3Jk8hH2I1QWvVV7Y8K','小红','/default/avatar.png','李小红',1,'2026-04-02 10:00:00','2026-05-05 09:30:00','127.0.0.1',1,0);

INSERT INTO `tb_user_asset` (`asset_id`,`user_id`,`integral`,`coupon_num`) VALUES
(1,1001,120,2),(2,1002,80,1);

INSERT INTO `tb_user_address` (`addr_id`,`user_id`,`consignee`,`phone`,`province`,`province_code`,`city`,`city_code`,`district`,`district_code`,`detail_addr`,`postal_code`,`remark`,`is_default`,`is_deleted`) VALUES
(1,1001,'张小明','13800000001','广东省','440000','深圳市','440300','南山区','440305','科技园 1 号楼 1201','518000','工作日送达',1,0),
(2,1001,'张小明','13800000001','广东省','440000','广州市','440100','天河区','440106','体育西路 88 号','510000','周末可收货',0,0),
(3,1002,'李小红','13800000002','浙江省','330000','杭州市','330100','西湖区','330106','文三路 99 号','310000','请放门卫',1,0);

INSERT INTO `tb_platform_admin` (`admin_id`,`admin_name`,`password`,`real_name`,`phone`,`status`,`last_login_time`,`login_ip`,`is_deleted`) VALUES
(1,'admin','$2a$10$7EqJtq98hPqEX7fNZaFWoOhi6WxPvq6WqZQ3Jk8hH2I1QWvVV7Y8K','平台管理员','13900000000',1,'2026-05-05 08:00:00','127.0.0.1',0);

INSERT INTO `tb_role` (`role_id`,`role_code`,`role_name`,`role_type`,`status`,`remark`,`is_deleted`) VALUES
(1,'SUPER_ADMIN','超级管理员',3,1,'拥有平台全部权限',0),
(2,'MERCHANT_MANAGER','商家运营',3,1,'负责商家和商品审核',0);

INSERT INTO `tb_permission` (`permission_id`,`permission_code`,`permission_name`,`permission_type`,`parent_id`,`path`,`method`,`sort_no`,`status`,`is_deleted`) VALUES
(1,'goods:audit','商品审核',3,0,'/api/admin/goods/*/audit','PUT',1,1,0),
(2,'merchant:audit','商家审核',3,0,'/api/admin/merchants/*/audit','PUT',2,1,0),
(3,'order:view','订单监管',3,0,'/api/admin/orders','GET',3,1,0),
(4,'dispute:handle','纠纷仲裁处理',3,0,'/api/admin/disputes/*','PUT',4,1,0);

INSERT INTO `tb_admin_role_relation` (`id`,`admin_id`,`role_id`) VALUES (1,1,1);
INSERT INTO `tb_role_permission_relation` (`id`,`role_id`,`permission_id`) VALUES (1,1,1),(2,1,2),(3,1,3),(4,1,4);

INSERT INTO `tb_merchant` (`merchant_id`,`merchant_name`,`merchant_type`,`legal_person`,`id_card`,`business_license`,`phone`,`email`,`address`,`password`,`shop_logo`,`shop_intro`,`audit_status`,`audit_remark`,`shop_score`,`status`,`register_time`,`audit_time`,`is_deleted`) VALUES
(2001,'数码优选旗舰店',2,'王数码','440***********001','/license/digital.png','13700000001','digital@example.com','深圳市南山区科技园','$2a$10$7EqJtq98hPqEX7fNZaFWoOhi6WxPvq6WqZQ3Jk8hH2I1QWvVV7Y8K','/shop/digital.png','主营手机、电脑与数码配件',1,'资料完整，审核通过',4.8,1,'2026-03-01 09:00:00','2026-03-02 10:00:00',0),
(2002,'潮流服饰馆',2,'陈服饰','330***********002','/license/fashion.png','13700000002','fashion@example.com','杭州市西湖区文三路','$2a$10$7EqJtq98hPqEX7fNZaFWoOhi6WxPvq6WqZQ3Jk8hH2I1QWvVV7Y8K','/shop/fashion.png','主营男女装和箱包鞋帽',1,'资料完整，审核通过',4.6,1,'2026-03-05 09:00:00','2026-03-06 10:00:00',0);

INSERT INTO `tb_merchant_audit_log` (`audit_log_id`,`merchant_id`,`before_status`,`after_status`,`audit_admin_id`,`audit_remark`) VALUES
(1,2001,0,1,1,'审核通过'),(2,2002,0,1,1,'审核通过');

INSERT INTO `tb_merchant_setting` (`setting_id`,`merchant_id`,`business_hours`,`refund_time`,`exchange_time`,`freight_template`) VALUES
(1,2001,'09:00-22:00',24,48,'{"baseFreight":0,"freeAmount":99}'),
(2,2002,'10:00-23:00',24,48,'{"baseFreight":8,"freeAmount":199}');

INSERT INTO `tb_merchant_finance` (`finance_id`,`merchant_id`,`shop_balance`,`unsettle_amount`,`commission_rate`,`freeze_amount`,`version`) VALUES
(1,2001,12000.00,560.00,0.0500,100.00,0),
(2,2002,8000.00,299.00,0.0500,0.00,0);

INSERT INTO `tb_category` (`cate_id`,`parent_cate_id`,`cate_name`,`cate_sort`,`status`,`is_deleted`) VALUES
(1,0,'数码产品',1,1,0),(2,1,'手机配件',2,1,0),(3,1,'电脑办公',3,1,0),(4,0,'服装鞋包',4,1,0),(5,4,'女装',5,1,0),(6,0,'家居日用',6,1,0);

INSERT INTO `tb_goods` (`goods_id`,`merchant_id`,`cate_id`,`goods_name`,`goods_intro`,`goods_pic`,`goods_video`,`keywords`,`audit_status`,`audit_remark`,`status`,`sell_count`,`comment_count`,`goods_score`,`is_deleted`) VALUES
(3001,2001,2,'快充数据线','支持 100W 快充，编织线材耐用','/goods/3001/main.png',NULL,'数据线,快充,Type-C',1,'审核通过',1,152,2,4.9,0),
(3002,2001,3,'无线蓝牙鼠标','静音按键，办公学习都适合','/goods/3002/main.png',NULL,'鼠标,蓝牙,办公',1,'审核通过',1,86,1,4.7,0),
(3003,2001,3,'轻薄笔记本电脑','16GB 内存，512GB 固态硬盘','/goods/3003/main.png',NULL,'笔记本,电脑,办公',1,'审核通过',1,32,1,4.8,0),
(3004,2002,5,'春季连衣裙','舒适面料，适合通勤和约会','/goods/3004/main.png',NULL,'连衣裙,女装,春季',1,'审核通过',1,210,1,4.6,0),
(3005,2002,4,'休闲帆布包','大容量，通勤旅行两用','/goods/3005/main.png',NULL,'帆布包,包包,通勤',1,'审核通过',1,65,0,4.5,0),
(3006,2001,2,'下架测试耳机','用于测试下架商品不可购买','/goods/3006/main.png',NULL,'耳机,测试',1,'审核通过',0,5,0,4.0,0);

INSERT INTO `tb_goods_audit_log` (`audit_log_id`,`goods_id`,`merchant_id`,`before_status`,`after_status`,`audit_admin_id`,`audit_remark`) VALUES
(1,3001,2001,0,1,1,'审核通过'),(2,3002,2001,0,1,1,'审核通过'),(3,3003,2001,0,1,1,'审核通过'),(4,3004,2002,0,1,1,'审核通过'),(5,3005,2002,0,1,1,'审核通过'),(6,3006,2001,0,1,1,'审核通过');

INSERT INTO `tb_goods_sku` (`sku_id`,`goods_id`,`sku_name`,`spec_params`,`price`,`stock`,`lock_stock`,`stock_warn`,`sku_code`,`status`,`version`,`is_deleted`) VALUES
(4001,3001,'1米-黑色','{"长度":"1米","颜色":"黑色"}',39.90,100,1,10,'SKU3001001',1,0,0),
(4002,3001,'2米-黑色','{"长度":"2米","颜色":"黑色"}',49.90,80,0,10,'SKU3001002',1,0,0),
(4003,3002,'银色','{"颜色":"银色"}',89.00,60,0,10,'SKU3002001',1,0,0),
(4004,3003,'16G+512G','{"内存":"16G","硬盘":"512G"}',4999.00,20,0,5,'SKU3003001',1,0,0),
(4005,3004,'M码-米白','{"尺码":"M","颜色":"米白"}',199.00,50,1,10,'SKU3004001',1,0,0),
(4006,3004,'L码-米白','{"尺码":"L","颜色":"米白"}',199.00,40,0,10,'SKU3004002',1,0,0),
(4007,3005,'卡其色','{"颜色":"卡其色"}',129.00,35,0,10,'SKU3005001',1,0,0),
(4008,3006,'标准版','{"版本":"标准版"}',299.00,0,0,10,'SKU3006001',0,0,0);

INSERT INTO `tb_goods_pic` (`pic_id`,`goods_id`,`pic_url`,`pic_sort`,`is_deleted`) VALUES
(1,3001,'/goods/3001/1.png',1,0),(2,3001,'/goods/3001/2.png',2,0),(3,3002,'/goods/3002/1.png',1,0),(4,3003,'/goods/3003/1.png',1,0),(5,3004,'/goods/3004/1.png',1,0),(6,3005,'/goods/3005/1.png',1,0);

INSERT INTO `tb_goods_price_log` (`log_id`,`goods_id`,`sku_id`,`old_price`,`new_price`,`operator_id`) VALUES
(1,3001,4001,45.90,39.90,2001),(2,3004,4005,219.00,199.00,2002);

INSERT INTO `tb_user_cart` (`cart_id`,`user_id`,`goods_id`,`sku_id`,`buy_num`,`is_selected`) VALUES
(1,1001,3001,4001,1,1),(2,1001,3002,4003,2,0),(3,1001,3004,4005,1,1),(4,1002,3003,4004,1,1),(5,1002,3005,4007,1,0);

INSERT INTO `tb_order_group` (`group_id`,`group_no`,`user_id`,`total_amount`,`discount_amount`,`freight_amount`,`pay_amount`,`pay_status`,`group_status`,`order_count`,`pay_type`,`pay_time`,`cancel_time`,`expire_time`,`create_time`) VALUES
(5001,'G202605050001',1001,89.80,10.00,0.00,79.80,1,1,1,9,'2026-05-05 10:05:00',NULL,'2026-05-05 10:30:00','2026-05-05 10:00:00'),
(5002,'G202605050002',1001,199.00,20.00,8.00,187.00,0,0,1,NULL,NULL,NULL,'2026-05-05 11:30:00','2026-05-05 11:00:00'),
(5003,'G202605050003',1002,4999.00,0.00,0.00,4999.00,1,2,1,9,'2026-05-05 09:10:00',NULL,'2026-05-05 09:30:00','2026-05-05 09:00:00');

INSERT INTO `tb_order` (`order_id`,`order_no`,`group_id`,`group_no`,`user_id`,`merchant_id`,`total_amount`,`pay_amount`,`discount_amount`,`freight`,`pay_type`,`pay_status`,`order_status`,`addr_id`,`consignee`,`consignee_phone`,`receive_addr`,`pay_time`,`delivery_time`,`receive_time`,`cancel_time`,`expire_time`,`buyer_remark`,`merchant_remark`,`create_time`) VALUES
(6001,'O202605050001',5001,'G202605050001',1001,2001,89.80,79.80,10.00,0.00,9,1,1,1,'张小明','13800000001','广东省深圳市南山区科技园 1 号楼 1201','2026-05-05 10:05:00',NULL,NULL,NULL,'2026-05-05 10:30:00','请尽快发货',NULL,'2026-05-05 10:00:00'),
(6002,'O202605050002',5002,'G202605050002',1001,2002,199.00,187.00,20.00,8.00,NULL,0,0,1,'张小明','13800000001','广东省深圳市南山区科技园 1 号楼 1201',NULL,NULL,NULL,NULL,'2026-05-05 11:30:00','颜色不要太深',NULL,'2026-05-05 11:00:00'),
(6003,'O202605050003',5003,'G202605050003',1002,2001,4999.00,4999.00,0.00,0.00,9,1,2,3,'李小红','13800000002','浙江省杭州市西湖区文三路 99 号','2026-05-05 09:10:00','2026-05-05 12:00:00',NULL,NULL,'2026-05-05 09:30:00',NULL,'顺丰已发','2026-05-05 09:00:00');

INSERT INTO `tb_order_item` (`order_item_id`,`order_id`,`group_id`,`merchant_id`,`goods_id`,`sku_id`,`goods_name`,`sku_name`,`goods_pic`,`price`,`num`,`total_price`,`comment_status`,`after_sale_status`) VALUES
(7001,6001,5001,2001,3001,4001,'快充数据线','1米-黑色','/goods/3001/main.png',39.90,1,39.90,1,0),
(7002,6001,5001,2001,3002,4003,'无线蓝牙鼠标','银色','/goods/3002/main.png',49.90,1,49.90,0,0),
(7003,6002,5002,2002,3004,4005,'春季连衣裙','M码-米白','/goods/3004/main.png',199.00,1,199.00,0,1),
(7004,6003,5003,2001,3003,4004,'轻薄笔记本电脑','16G+512G','/goods/3003/main.png',4999.00,1,4999.00,0,0);

INSERT INTO `tb_payment` (`payment_id`,`pay_no`,`group_id`,`order_id`,`user_id`,`pay_amount`,`pay_channel`,`pay_status`,`third_trade_no`,`pay_time`,`expire_time`,`create_time`) VALUES
(8001,'P202605050001',5001,6001,1001,79.80,9,1,'MOCK202605050001','2026-05-05 10:05:00','2026-05-05 10:30:00','2026-05-05 10:00:00'),
(8002,'P202605050002',5002,6002,1001,187.00,9,0,NULL,NULL,'2026-05-05 11:30:00','2026-05-05 11:00:00'),
(8003,'P202605050003',5003,6003,1002,4999.00,9,1,'MOCK202605050003','2026-05-05 09:10:00','2026-05-05 09:30:00','2026-05-05 09:00:00');

INSERT INTO `tb_order_status_log` (`log_id`,`target_type`,`group_id`,`order_id`,`before_status`,`after_status`,`before_pay_status`,`after_pay_status`,`operator_type`,`operator_id`,`operation_desc`) VALUES
(1,1,5001,NULL,NULL,0,NULL,0,1,1001,'创建父订单'),
(2,2,5001,6001,NULL,0,NULL,0,1,1001,'创建子订单'),
(3,1,5001,NULL,0,1,0,1,4,NULL,'模拟支付成功'),
(4,2,5001,6001,0,1,0,1,4,NULL,'模拟支付成功'),
(5,2,5003,6003,1,2,1,1,2,2001,'商家发货');

INSERT INTO `tb_logistics` (`logistics_id`,`order_id`,`merchant_id`,`express_company`,`express_no`,`logistics_status`,`predict_receive_time`,`sign_time`) VALUES
(9001,6003,2001,'顺丰速运','SF1000000001',2,'2026-05-07 18:00:00',NULL);

INSERT INTO `tb_logistics_trace` (`trace_id`,`logistics_id`,`trace_content`,`trace_time`,`trace_location`) VALUES
(1,9001,'商家已发货，等待快递揽收','2026-05-05 12:00:00','深圳南山'),
(2,9001,'快件已揽收','2026-05-05 14:00:00','深圳南山集散点'),
(3,9001,'快件运输中','2026-05-05 20:00:00','深圳转运中心');

INSERT INTO `tb_coupon` (`coupon_id`,`coupon_type`,`merchant_id`,`coupon_name`,`discount_type`,`denomination`,`discount_rate`,`min_amount`,`per_limit`,`can_stack`,`start_time`,`end_time`,`total_num`,`surplus_num`,`audit_status`,`status`,`is_deleted`) VALUES
(10001,1,NULL,'平台满50减10',1,10.00,NULL,50.00,1,0,'2026-05-01 00:00:00','2026-12-31 23:59:59',1000,998,1,1,0),
(10002,2,2002,'潮流服饰满100减20',1,20.00,NULL,100.00,1,0,'2026-05-01 00:00:00','2026-12-31 23:59:59',500,498,1,1,0),
(10003,1,NULL,'平台8折券',2,0.00,0.8000,200.00,1,0,'2026-05-01 00:00:00','2026-12-31 23:59:59',300,300,1,1,0);

INSERT INTO `tb_coupon_scope` (`scope_id`,`coupon_id`,`scope_type`,`target_id`) VALUES
(1,10001,1,0),(2,10002,2,2002),(3,10003,1,0);

INSERT INTO `tb_user_coupon` (`user_coupon_id`,`user_id`,`coupon_id`,`merchant_id`,`use_status`,`lock_group_id`,`use_group_id`,`use_order_id`,`use_time`,`receive_time`,`expire_time`) VALUES
(11001,1001,10001,NULL,1,NULL,5001,6001,'2026-05-05 10:05:00','2026-05-01 10:00:00','2026-12-31 23:59:59'),
(11002,1001,10002,2002,3,5002,NULL,NULL,NULL,'2026-05-02 10:00:00','2026-12-31 23:59:59'),
(11003,1002,10003,NULL,0,NULL,NULL,NULL,NULL,'2026-05-03 10:00:00','2026-12-31 23:59:59');

INSERT INTO `tb_order_coupon` (`order_coupon_id`,`group_id`,`order_id`,`user_id`,`coupon_id`,`user_coupon_id`,`coupon_name`,`discount_amount`) VALUES
(1,5001,6001,1001,10001,11001,'平台满50减10',10.00);

INSERT INTO `tb_after_sale` (`after_sale_id`,`after_sale_no`,`group_id`,`order_id`,`order_item_id`,`user_id`,`merchant_id`,`after_sale_type`,`apply_reason`,`apply_evidence`,`apply_amount`,`handle_status`,`merchant_remark`,`platform_remark`,`handle_time`,`apply_time`) VALUES
(12001,'AS202605050001',5002,6002,7003,1001,2002,4,'尺码不合适，申请退货退款','/evidence/as12001-1.png,/evidence/as12001-2.png',187.00,3,'需要平台介入确认商品状态',NULL,NULL,'2026-05-05 12:00:00');

INSERT INTO `tb_after_sale_log` (`log_id`,`after_sale_id`,`before_status`,`after_status`,`operator_type`,`operator_id`,`operation_desc`) VALUES
(1,12001,NULL,0,1,1001,'用户提交售后申请'),
(2,12001,0,3,2,2002,'商家申请平台介入');

INSERT INTO `tb_after_sale_logistics` (`asl_id`,`after_sale_id`,`user_id`,`merchant_id`,`express_company`,`express_no`,`send_time`,`receive_time`,`logistics_status`) VALUES
(1,12001,1001,2002,'中通快递','ZTO10000001','2026-05-05 15:00:00',NULL,1);

INSERT INTO `tb_dispute` (`dispute_id`,`dispute_no`,`after_sale_id`,`group_id`,`order_id`,`order_item_id`,`user_id`,`merchant_id`,`apply_type`,`dispute_reason`,`dispute_desc`,`dispute_status`,`judge_result`,`final_amount`,`platform_admin_id`,`platform_opinion`,`create_time`,`judge_time`) VALUES
(13001,'D202605050001',12001,5002,6002,7003,1001,2002,1,'退货退款争议','用户认为商品尺码描述不清，商家认为影响二次销售，申请平台仲裁。',1,NULL,NULL,1,NULL,'2026-05-05 13:00:00',NULL);

INSERT INTO `tb_dispute_evidence` (`evidence_id`,`dispute_id`,`submitter_type`,`submitter_id`,`evidence_type`,`evidence_content`,`remark`,`is_valid`) VALUES
(1,13001,1,1001,1,'/evidence/dispute/13001-user-1.png','用户上传商品照片',1),
(2,13001,2,2002,2,'聊天记录截图：/evidence/dispute/13001-chat.png','商家上传沟通截图',1);

INSERT INTO `tb_refund` (`refund_id`,`refund_no`,`payment_id`,`group_id`,`order_id`,`after_sale_id`,`user_id`,`merchant_id`,`refund_amount`,`refund_status`,`refund_channel`,`third_refund_no`,`reason`,`apply_time`,`success_time`) VALUES
(14001,'R202605050001',8002,5002,6002,12001,1001,2002,187.00,0,9,NULL,'售后退货退款，待平台仲裁','2026-05-05 13:30:00',NULL);

INSERT INTO `tb_merchant_account_flow` (`flow_id`,`flow_no`,`merchant_id`,`order_id`,`refund_id`,`withdraw_id`,`flow_type`,`amount`,`balance_after`,`freeze_after`,`remark`) VALUES
(1,'F202605050001',2001,6001,NULL,NULL,1,75.81,12075.81,100.00,'订单入账，扣除平台佣金后金额'),
(2,'F202605050002',2002,6002,14001,NULL,3,-187.00,7813.00,0.00,'售后退款冻结/扣减');

INSERT INTO `tb_sku_stock_log` (`stock_log_id`,`sku_id`,`goods_id`,`merchant_id`,`change_type`,`change_num`,`before_stock`,`after_stock`,`related_order_id`,`related_order_item_id`,`operator_type`,`operator_id`,`remark`) VALUES
(1,4001,3001,2001,2,-1,101,100,6001,7001,4,NULL,'支付成功扣减实际库存'),
(2,4005,3004,2002,2,0,50,50,6002,7003,4,NULL,'待支付订单锁定库存，实际库存未扣减');

INSERT INTO `tb_goods_comment` (`comment_id`,`order_item_id`,`user_id`,`goods_id`,`merchant_id`,`goods_score`,`service_score`,`logistics_score`,`comment_content`,`comment_pic`,`is_top`,`is_valid`,`comment_time`,`reply_content`,`reply_time`) VALUES
(1,7001,1001,3001,2001,5,5,5,'数据线很好用，充电很快。','/comment/7001-1.png,/comment/7001-2.png',0,1,'2026-05-05 15:00:00','感谢支持，欢迎下次光临。','2026-05-05 16:00:00'),
(2,7004,1002,3003,2001,5,5,4,'电脑性能不错，物流也很快。',NULL,0,1,'2026-05-05 18:00:00',NULL,NULL);

INSERT INTO `tb_user_collect` (`collect_id`,`user_id`,`goods_id`,`is_cancel`) VALUES
(1,1001,3001,0),(2,1001,3003,0),(3,1001,3004,0),(4,1002,3005,0);

INSERT INTO `tb_user_browse_history` (`history_id`,`user_id`,`goods_id`,`sku_id`,`merchant_id`,`source_type`,`browse_count`,`last_browse_time`,`is_deleted`) VALUES
(1,1001,3001,4001,2001,3,5,'2026-05-05 09:40:00',0),
(2,1001,3004,4005,2002,4,3,'2026-05-05 10:40:00',0),
(3,1001,3003,4004,2001,1,2,'2026-05-05 11:20:00',0),
(4,1002,3005,4007,2002,2,2,'2026-05-05 12:00:00',0);

INSERT INTO `tb_search_history` (`search_id`,`user_id`,`keyword`,`result_count`,`search_source`,`create_time`) VALUES
(1,1001,'数据线',3,1,'2026-05-05 09:30:00'),(2,1001,'连衣裙',2,1,'2026-05-05 10:30:00'),(3,1002,'笔记本',1,1,'2026-05-05 11:00:00');

INSERT INTO `tb_hot_search_keyword` (`hot_id`,`keyword`,`search_count`,`hot_score`,`stat_date`,`status`) VALUES
(1,'数据线',120,98.5000,'2026-05-05',1),(2,'连衣裙',95,88.0000,'2026-05-05',1),(3,'笔记本电脑',80,85.3000,'2026-05-05',1);

INSERT INTO `tb_recommend_record` (`record_id`,`user_id`,`goods_id`,`scene`,`reason`,`score`,`is_click`,`is_buy`,`create_time`) VALUES
(1,1001,3001,1,'根据最近浏览的数码分类推荐',0.9500,1,1,'2026-05-05 09:50:00'),
(2,1001,3003,1,'根据收藏和浏览记录推荐',0.9100,0,0,'2026-05-05 10:20:00'),
(3,1002,3005,1,'根据服饰分类热度推荐',0.8700,1,0,'2026-05-05 11:20:00');

INSERT INTO `tb_goods_rank_snapshot` (`rank_id`,`rank_type`,`cate_id`,`goods_id`,`rank_no`,`rank_score`,`snapshot_date`) VALUES
(1,1,0,3004,1,210.0000,'2026-05-05'),(2,1,0,3001,2,152.0000,'2026-05-05'),(3,3,0,3001,1,4.9000,'2026-05-05'),(4,4,0,3003,1,90.0000,'2026-05-05');

INSERT INTO `tb_live` (`live_id`,`merchant_id`,`live_title`,`live_cover`,`live_theme`,`start_time`,`end_time`,`live_status`,`watch_num`,`interact_num`,`is_deleted`) VALUES
(15001,2001,'数码好物直播间','/live/15001/cover.png','数码配件专场','2026-05-05 19:00:00',NULL,1,238,52,0),
(15002,2002,'春季穿搭直播','/live/15002/cover.png','女装穿搭专场','2026-05-06 20:00:00',NULL,0,0,0,0);

INSERT INTO `tb_live_goods` (`lg_id`,`live_id`,`goods_id`,`sku_id`,`live_price`,`goods_sort`,`is_on_shelf`) VALUES
(1,15001,3001,4001,35.90,1,1),(2,15001,3002,4003,79.00,2,1),(3,15002,3004,4005,179.00,1,1),(4,15002,3005,4007,109.00,2,1);

INSERT INTO `tb_live_comment` (`comment_id`,`live_id`,`user_id`,`content`,`comment_type`,`is_valid`,`create_time`) VALUES
(1,15001,1001,'这个数据线支持苹果手机吗？',2,1,'2026-05-05 19:05:00'),
(2,15001,1002,'价格不错，先收藏了。',1,1,'2026-05-05 19:06:00'),
(3,15001,1001,'已下单，期待发货。',1,1,'2026-05-05 19:10:00');

INSERT INTO `tb_live_like` (`like_id`,`live_id`,`user_id`,`like_status`) VALUES
(1,15001,1001,1),(2,15001,1002,1);

INSERT INTO `tb_live_watch_record` (`watch_id`,`live_id`,`user_id`,`enter_time`,`leave_time`,`watch_seconds`) VALUES
(1,15001,1001,'2026-05-05 19:00:00','2026-05-05 19:15:00',900),
(2,15001,1002,'2026-05-05 19:02:00',NULL,0);

INSERT INTO `tb_banner` (`banner_id`,`banner_title`,`image_url`,`jump_type`,`jump_value`,`display_position`,`sort_no`,`status`,`start_time`,`end_time`,`is_deleted`) VALUES
(1,'数码好物节','/banner/digital-sale.png',3,'1',1,1,1,'2026-05-01 00:00:00','2026-05-31 23:59:59',0),
(2,'春季穿搭上新','/banner/fashion-spring.png',2,'5',1,2,1,'2026-05-01 00:00:00','2026-05-31 23:59:59',0);

INSERT INTO `tb_home_section` (`section_id`,`section_name`,`section_type`,`section_sort`,`status`,`is_deleted`) VALUES
(1,'热门推荐',1,1,1,0),(2,'数码专区',1,2,1,0),(3,'直播精选',4,3,1,0);

INSERT INTO `tb_home_section_goods` (`id`,`section_id`,`goods_id`,`sort_no`,`status`) VALUES
(1,1,3001,1,1),(2,1,3004,2,1),(3,2,3002,1,1),(4,2,3003,2,1);

INSERT INTO `tb_platform_notice` (`notice_id`,`notice_title`,`notice_content`,`notice_type`,`is_top`,`status`,`create_admin_id`,`is_deleted`,`publish_time`) VALUES
(1,'平台系统维护通知','平台将在本周日凌晨进行短暂维护。',1,1,1,1,0,'2026-05-05 08:00:00');

INSERT INTO `tb_platform_activity` (`activity_id`,`activity_name`,`activity_type`,`start_time`,`end_time`,`activity_rule`,`status`,`create_admin_id`,`is_deleted`) VALUES
(1,'五月购物节',1,'2026-05-01 00:00:00','2026-05-31 23:59:59','{"type":"full_reduce","minAmount":100,"discount":20}',1,1,0);

INSERT INTO `tb_merchant_activity` (`ma_id`,`activity_id`,`merchant_id`,`goods_id`,`audit_status`,`audit_remark`,`sign_up_time`,`audit_time`) VALUES
(1,1,2001,3001,1,'报名通过','2026-04-28 10:00:00','2026-04-29 10:00:00'),(2,1,2002,3004,1,'报名通过','2026-04-28 11:00:00','2026-04-29 11:00:00');

INSERT INTO `tb_message` (`message_id`,`receiver_type`,`receiver_id`,`message_type`,`title`,`content`,`related_type`,`related_id`,`is_read`,`read_time`,`is_deleted`,`create_time`) VALUES
(1,1,1001,1,'订单支付成功','您的订单 O202605050001 已支付成功。',1,6001,0,NULL,0,'2026-05-05 10:05:00'),
(2,2,2001,1,'新订单提醒','您有新的待发货订单 O202605050001。',1,6001,0,NULL,0,'2026-05-05 10:05:00'),
(3,1,1001,2,'售后已进入平台介入','您的售后单 AS202605050001 已进入平台介入阶段。',2,12001,0,NULL,0,'2026-05-05 13:00:00');

INSERT INTO `tb_merchant_ai_rule` (`rule_id`,`merchant_id`,`rule_name`,`match_type`,`keywords`,`reply_content`,`priority`,`work_time_start`,`work_time_end`,`status`,`is_deleted`) VALUES
(1,2001,'物流咨询自动回复',1,'物流,快递,发货','您好，本店一般在支付后24小时内发货，可在订单详情查看物流。',10,'09:00:00','22:00:00',1,0),
(2,2002,'营业时间外自动回复',4,NULL,'您好，当前客服不在线，我们会在营业时间尽快回复您。',1,'22:00:00','10:00:00',1,0);

INSERT INTO `tb_chat_session` (`session_id`,`user_id`,`merchant_id`,`session_status`,`last_message_id`,`last_message_content`,`last_message_time`,`user_unread_count`,`merchant_unread_count`) VALUES
(1,1001,2001,1,2,'您好，本店一般在支付后24小时内发货，可在订单详情查看物流。','2026-05-05 14:01:00',0,0),
(2,1001,2002,1,3,'您好，售后问题我们会尽快处理。','2026-05-05 14:05:00',0,1);

INSERT INTO `tb_chat_message` (`message_id`,`session_id`,`sender_type`,`sender_id`,`receiver_type`,`receiver_id`,`message_type`,`content`,`related_type`,`related_id`,`is_read`,`read_time`,`is_deleted`,`create_time`) VALUES
(1,1,1,1001,2,2001,1,'什么时候发货？',1,6001,1,'2026-05-05 14:01:00',0,'2026-05-05 14:00:00'),
(2,1,3,NULL,1,1001,1,'您好，本店一般在支付后24小时内发货，可在订单详情查看物流。',NULL,NULL,1,'2026-05-05 14:01:30',0,'2026-05-05 14:01:00'),
(3,2,2,2002,1,1001,1,'您好，售后问题我们会尽快处理。',2,12001,0,NULL,0,'2026-05-05 14:05:00');

INSERT INTO `tb_merchant_push_task` (`task_id`,`task_no`,`merchant_id`,`task_title`,`task_content`,`target_type`,`target_condition`,`related_type`,`related_id`,`task_status`,`plan_send_time`,`actual_send_time`,`success_count`,`fail_count`) VALUES
(1,'MT202605050001',2001,'数码好物直播提醒','今晚19点数码优选直播间开播，数据线低至35.9元。',1,'{"userRange":"paid_or_collected"}',3,15001,3,'2026-05-05 18:30:00','2026-05-05 18:30:10',2,0);

INSERT INTO `tb_platform_daily_stat` (`stat_id`,`stat_date`,`new_user_count`,`new_merchant_count`,`order_count`,`pay_order_count`,`gross_sales`,`pay_amount`,`refund_amount`) VALUES
(1,'2026-05-05',2,0,3,2,5287.80,5078.80,0.00);

INSERT INTO `tb_merchant_daily_stat` (`stat_id`,`merchant_id`,`stat_date`,`goods_view_count`,`order_count`,`pay_order_count`,`sales_amount`,`refund_amount`,`new_comment_count`) VALUES
(1,2001,'2026-05-05',320,2,2,5078.80,0.00,2),(2,2002,'2026-05-05',180,1,0,199.00,0.00,0);

INSERT INTO `tb_goods_daily_stat` (`stat_id`,`goods_id`,`merchant_id`,`stat_date`,`view_count`,`collect_count`,`cart_count`,`order_count`,`pay_count`,`sales_amount`,`comment_count`) VALUES
(1,3001,2001,'2026-05-05',120,20,15,1,1,39.90,1),(2,3003,2001,'2026-05-05',90,12,3,1,1,4999.00,1),(3,3004,2002,'2026-05-05',150,18,8,1,0,199.00,0);

INSERT INTO `tb_abnormal_order` (`abnormal_id`,`order_id`,`abnormal_type`,`abnormal_desc`,`handle_status`,`handle_admin_id`,`create_time`,`handle_time`) VALUES
(1,6002,2,'用户短时间多次申请售后，进入平台观察。',0,NULL,'2026-05-05 13:20:00',NULL);

INSERT INTO `tb_violation` (`violation_id`,`violation_type`,`target_id`,`violation_content`,`violation_grade`,`punish_type`,`punish_days`,`handle_admin_id`,`handle_remark`,`handle_time`,`status`,`unlock_time`) VALUES
(1,3,3006,'商品信息不完整，下架整改。',1,3,7,1,'要求商家补充详情页信息。','2026-05-05 08:30:00',1,'2026-05-12 08:30:00');

INSERT INTO `tb_operation_log` (`log_id`,`operator_type`,`operator_id`,`operation_module`,`operation_content`,`operation_ip`,`operation_result`) VALUES
(1,3,1,'商品审核','审核通过商品 3001','127.0.0.1',1),
(2,3,1,'纠纷仲裁','创建纠纷单 D202605050001','127.0.0.1',1);

SET FOREIGN_KEY_CHECKS = 1;
