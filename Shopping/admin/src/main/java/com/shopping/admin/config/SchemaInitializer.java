package com.shopping.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        initOperationLogTable();
        initSystemConfigTable();
        alterCouponTable();
        alterAbnormalOrderTable();
        alterAfterSaleTable();
        initNotificationTable();
        initRolePermissionData();
        initChatBotLogTable();
        initBannerData();
    }

    private void initOperationLogTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_operation_log` (" +
                    "`log_id` bigint NOT NULL AUTO_INCREMENT," +
                    "`operator_type` tinyint DEFAULT '1' COMMENT '操作人类型：1-平台管理员 2-商家 3-用户'," +
                    "`operator_id` bigint DEFAULT NULL COMMENT '操作人ID'," +
                    "`operation_module` varchar(50) DEFAULT NULL COMMENT '操作模块'," +
                    "`operation_content` varchar(1000) DEFAULT NULL COMMENT '操作内容'," +
                    "`operation_ip` varchar(50) DEFAULT NULL COMMENT '操作IP'," +
                    "`operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'," +
                    "`operation_result` tinyint DEFAULT '1' COMMENT '操作结果：1-成功 0-失败'," +
                    "PRIMARY KEY (`log_id`)," +
                    "KEY `idx_operator_id` (`operator_id`)," +
                    "KEY `idx_operation_module` (`operation_module`)," +
                    "KEY `idx_operation_time` (`operation_time`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
            log.info("操作日志表初始化完成");
        } catch (Exception e) {
            log.warn("操作日志表初始化异常: {}", e.getMessage());
        }
    }

    private void initSystemConfigTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_system_config` (" +
                    "`config_id` bigint NOT NULL AUTO_INCREMENT," +
                    "`config_key` varchar(100) NOT NULL," +
                    "`config_value` varchar(500) DEFAULT ''," +
                    "`config_desc` varchar(200) DEFAULT NULL," +
                    "`config_group` varchar(50) NOT NULL DEFAULT 'basic'," +
                    "`sort_no` int DEFAULT '0'," +
                    "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`config_id`)," +
                    "UNIQUE KEY `uk_config_key` (`config_key`)," +
                    "KEY `idx_config_group` (`config_group`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");

            String[][] configs = {
                    {"site_name", "电商管理平台", "站点名称", "basic", "1"},
                    {"site_logo", "", "站点Logo URL", "basic", "2"},
                    {"site_icp", "", "ICP备案号", "basic", "3"},
                    {"site_contact", "400-888-8888", "客服电话", "basic", "4"},
                    {"site_email", "admin@shop.com", "管理员邮箱", "basic", "5"},
                    {"order_auto_cancel_minutes", "30", "订单自动取消时间(分钟)", "order", "1"},
                    {"order_auto_confirm_days", "7", "发货后自动确认收货天数", "order", "2"},
                    {"order_return_days", "7", "退货申请天数限制", "order", "3"},
                    {"payment_alipay_enabled", "1", "支付宝支付开启", "payment", "1"},
                    {"payment_wechat_enabled", "1", "微信支付开启", "payment", "2"},
                    {"shipping_default_fee", "0", "默认运费(元)", "shipping", "1"},
                    {"shipping_free_threshold", "99", "包邮门槛(元)", "shipping", "2"},
                    {"shipping_company", "顺丰速运", "默认物流公司", "shipping", "3"}
            };
            for (String[] c : configs) {
                jdbcTemplate.update(
                        "INSERT IGNORE INTO `tb_system_config` (`config_key`, `config_value`, `config_desc`, `config_group`, `sort_no`) VALUES (?,?,?,?,?)",
                        c[0], c[1], c[2], c[3], Integer.parseInt(c[4]));
            }
            log.info("系统配置表初始化完成");
        } catch (Exception e) {
            log.warn("系统配置表初始化异常: {}", e.getMessage());
        }
    }

    private void alterCouponTable() {
        try {
            // 安全添加列（如果不存在）
            String[][] columns = {
                    {"scope_type", "tinyint DEFAULT '1' COMMENT '适用范围：1-全场 2-指定分类 3-指定商品'"},
                    {"scope_ids", "varchar(500) DEFAULT NULL COMMENT '适用范围ID列表'"},
                    {"per_limit", "int DEFAULT '0' COMMENT '每人限领数量'"},
                    {"usage_desc", "varchar(500) DEFAULT NULL COMMENT '使用说明'"}
            };
            for (String[] col : columns) {
                try {
                    jdbcTemplate.execute("ALTER TABLE `tb_coupon` ADD COLUMN `" + col[0] + "` " + col[1]);
                } catch (Exception ignored) {
                    // 列已存在则忽略
                }
            }
            log.info("优惠券表字段补全完成");
        } catch (Exception e) {
            log.warn("优惠券表字段补全异常: {}", e.getMessage());
        }
    }

    private void alterAbnormalOrderTable() {
        try {
            try {
                jdbcTemplate.execute("ALTER TABLE `tb_abnormal_order` ADD COLUMN `handle_remark` varchar(500) DEFAULT NULL COMMENT '处理备注'");
            } catch (Exception ignored) {}
            log.info("异常订单表字段补全完成");
        } catch (Exception e) {
            log.warn("异常订单表字段补全异常: {}", e.getMessage());
        }
    }

    private void alterAfterSaleTable() {
        try {
            try {
                jdbcTemplate.execute("ALTER TABLE `tb_after_sale` ADD COLUMN `handle_admin_id` bigint DEFAULT NULL COMMENT '处理管理员ID'");
            } catch (Exception ignored) {}
            log.info("售后表字段补全完成");
        } catch (Exception e) {
            log.warn("售后表字段补全异常: {}", e.getMessage());
        }
    }

    private void initNotificationTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_notification` (" +
                    "`notification_id` bigint NOT NULL AUTO_INCREMENT," +
                    "`admin_id` bigint DEFAULT NULL COMMENT '管理员ID，null表示全体'," +
                    "`title` varchar(200) NOT NULL COMMENT '标题'," +
                    "`content` varchar(1000) DEFAULT NULL COMMENT '内容'," +
                    "`type` tinyint NOT NULL DEFAULT '1' COMMENT '1-系统通知 2-待办提醒 3-审核通知 4-预警通知 5-订单'," +
                    "`related_id` bigint DEFAULT NULL COMMENT '关联业务ID'," +
                    "`related_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型'," +
                    "`is_read` tinyint NOT NULL DEFAULT '0' COMMENT '0-未读 1-已读'," +
                    "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`notification_id`)," +
                    "KEY `idx_admin_id` (`admin_id`)," +
                    "KEY `idx_is_read` (`is_read`)," +
                    "KEY `idx_create_time` (`create_time`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
            try { jdbcTemplate.execute("ALTER TABLE `tb_notification` ADD COLUMN `related_id` bigint DEFAULT NULL COMMENT '关联业务ID'"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("ALTER TABLE `tb_notification` ADD COLUMN `related_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型'"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("UPDATE `tb_notification` SET `related_id` = `biz_id` WHERE `related_id` IS NULL AND `biz_id` IS NOT NULL"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("UPDATE `tb_notification` SET `related_type` = `biz_type` WHERE `related_type` IS NULL AND `biz_type` IS NOT NULL"); } catch (Exception ignored) {}
            log.info("通知表初始化完成");

        } catch (Exception e) {
            log.warn("通知表初始化异常: {}", e.getMessage());
        }
    }

    private void initRolePermissionData() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_permission` (" +
                    "`permission_id` bigint NOT NULL AUTO_INCREMENT," +
                    "`permission_name` varchar(50) NOT NULL," +
                    "`permission_code` varchar(64) NOT NULL," +
                    "`module` varchar(32) NOT NULL," +
                    "`permission_type` tinyint NOT NULL," +
                    "`parent_id` bigint DEFAULT '0'," +
                    "`sort_no` int DEFAULT '0'," +
                    "`status` tinyint NOT NULL DEFAULT '1'," +
                    "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "`is_deleted` tinyint NOT NULL DEFAULT '0'," +
                    "PRIMARY KEY (`permission_id`)," +
                    "UNIQUE KEY `uk_permission_code` (`permission_code`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_role_permission` (" +
                    "`id` bigint NOT NULL AUTO_INCREMENT," +
                    "`role_id` bigint NOT NULL," +
                    "`permission_id` bigint NOT NULL," +
                    "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_role_id` (`role_id`)," +
                    "KEY `idx_permission_id` (`permission_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
            try { jdbcTemplate.execute("ALTER TABLE `tb_role` ADD COLUMN `data_scope` tinyint DEFAULT 1 COMMENT '数据范围：1-全部 2-本部门 3-本人'"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("ALTER TABLE `tb_role` ADD COLUMN `role_type` tinyint DEFAULT 0 COMMENT '角色类型：0-自定义 1-系统内置'"); } catch (Exception ignored) {}

            Object[][] permissions = {
                    {1, "系统配置", "SYSTEM_CONFIG", "SYSTEM", 1, 0, 1}, {2, "角色管理", "SYSTEM_ROLE", "SYSTEM", 1, 0, 2}, {3, "员工管理", "ADMIN_MGMT", "SYSTEM", 1, 0, 3}, {4, "操作日志", "LOG_VIEW", "SYSTEM", 1, 0, 4},
                    {5, "首页看板", "DASHBOARD_VIEW", "DATA", 1, 0, 5},
                    {10, "用户管理", "USER_MGMT", "USER", 1, 0, 10}, {11, "用户查看", "USER_VIEW", "USER", 2, 10, 1}, {12, "用户禁用", "USER_DISABLE", "USER", 2, 10, 2}, {14, "用户风控", "USER_RISK", "USER", 2, 10, 4},
                    {20, "商家管理", "MERCHANT_MGMT", "MERCHANT", 1, 0, 20}, {21, "商家查看", "MERCHANT_VIEW", "MERCHANT", 2, 20, 1}, {22, "商家审核", "MERCHANT_AUDIT", "MERCHANT", 2, 20, 2}, {23, "商家冻结", "MERCHANT_FREEZE", "MERCHANT", 2, 20, 3}, {25, "商家信用管理", "MERCHANT_CREDIT", "MERCHANT", 2, 20, 5},
                    {30, "商品管理", "GOODS_MGMT", "GOODS", 1, 0, 30}, {31, "商品查看", "GOODS_VIEW", "GOODS", 2, 30, 1}, {32, "商品审核", "GOODS_AUDIT", "GOODS", 2, 30, 2}, {33, "商品下架", "GOODS_OFFLINE", "GOODS", 2, 30, 3}, {34, "商品风控", "GOODS_RISK", "GOODS", 2, 30, 4}, {35, "类目管理", "GOODS_CATEGORY", "GOODS", 2, 30, 5},
                    {40, "订单管理", "ORDER_MGMT", "ORDER", 1, 0, 40}, {41, "订单查看", "ORDER_VIEW", "ORDER", 2, 40, 1}, {42, "异常订单处理", "ORDER_ABNORMAL", "ORDER", 2, 40, 2}, {43, "订单干预", "ORDER_INTERVENE", "ORDER", 2, 40, 3}, {44, "提醒商家发货", "ORDER_REMIND_SHIP", "ORDER", 2, 40, 4}, {45, "订单退款", "ORDER_REFUND", "ORDER", 2, 40, 5},
                    {50, "数据中心", "DATA_MGMT", "DATA", 1, 0, 50}, {51, "数据查看", "DATA_VIEW", "DATA", 2, 50, 1}, {52, "报表查看", "REPORT_VIEW", "DATA", 2, 50, 2},
                    {60, "营销管理", "MARKETING_MGMT", "MARKETING", 1, 0, 60}, {61, "优惠券管理", "MARKETING_COUPON", "MARKETING", 2, 60, 1}, {62, "活动管理", "MARKETING_ACTIVITY", "MARKETING", 2, 60, 2},
                    {70, "内容运营", "CONTENT_MGMT", "CONTENT", 1, 0, 70}, {71, "轮播图管理", "CONTENT_BANNER", "CONTENT", 2, 70, 1}, {72, "评论管理", "REVIEW_MGMT", "CONTENT", 2, 70, 2},
                    {80, "纠纷仲裁", "DISPUTE_MGMT", "DISPUTE", 1, 0, 80}, {81, "纠纷处理", "DISPUTE_HANDLE", "DISPUTE", 2, 80, 1}, {82, "售后管理", "AFTER_SALE_MGMT", "DISPUTE", 1, 0, 82}, {83, "售后处理", "AFTER_SALE_HANDLE", "DISPUTE", 2, 82, 1},
                    {90, "财务结算", "FINANCE_MGMT", "FINANCE", 1, 0, 90}, {91, "结算查看", "FINANCE_VIEW", "FINANCE", 2, 90, 1}, {92, "对账管理", "FINANCE_RECONCILE", "FINANCE", 2, 90, 2},
                    {100, "消息中心", "MESSAGE_MGMT", "MESSAGE", 1, 0, 100}, {101, "客服消息", "CHAT_MGMT", "MESSAGE", 1, 100, 1}, {102, "消息模板", "MESSAGE_TEMPLATE", "MESSAGE", 2, 100, 2}
            };
            for (Object[] p : permissions) {
                jdbcTemplate.update("INSERT IGNORE INTO `tb_permission` (`permission_id`,`permission_name`,`permission_code`,`module`,`permission_type`,`parent_id`,`sort_no`) VALUES (?,?,?,?,?,?,?)", p);
            }

            Object[][] roles = {
                    {1, "超级管理员", "SUPER_ADMIN", "拥有系统所有权限", 1, 1, 1},
                    {2, "运营管理员", "OPERATOR", "负责商家、商品、订单、营销和内容运营", 1, 1, 1},
                    {3, "客服", "SERVICE", "负责客服消息、售后、退款、纠纷和发货提醒", 1, 3, 1},
                    {4, "审计员", "AUDITOR", "负责数据报表、日志审计和只读巡检", 1, 2, 1},
                    {5, "风控专员", "RISK_OFFICER", "负责异常订单、商家信用和用户风控", 1, 2, 1},
                    {6, "财务专员", "FINANCE_OFFICER", "负责退款、结算、对账和财务数据", 1, 2, 1},
                    {7, "内容审核员", "CONTENT_REVIEWER", "负责商品图文、评论和内容审核", 1, 2, 1}
            };
            for (Object[] r : roles) {
                jdbcTemplate.update("INSERT INTO `tb_role` (`role_id`,`role_name`,`role_code`,`description`,`status`,`data_scope`,`role_type`) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`),`description`=VALUES(`description`),`status`=VALUES(`status`),`data_scope`=VALUES(`data_scope`),`role_type`=VALUES(`role_type`)", r);
            }
            jdbcTemplate.update("INSERT IGNORE INTO `tb_admin_role_relation` (`admin_id`,`role_id`) VALUES (1,1)");
            jdbcTemplate.update("INSERT IGNORE INTO `tb_role_permission` (`role_id`, `permission_id`) SELECT 1, `permission_id` FROM `tb_permission`");
            seedRolePermissions(2, "MERCHANT_MGMT", "MERCHANT_VIEW", "MERCHANT_AUDIT", "MERCHANT_FREEZE", "MERCHANT_CREDIT", "GOODS_MGMT", "GOODS_VIEW", "GOODS_AUDIT", "GOODS_OFFLINE", "GOODS_CATEGORY", "ORDER_MGMT", "ORDER_VIEW", "ORDER_REMIND_SHIP", "MARKETING_MGMT", "MARKETING_COUPON", "MARKETING_ACTIVITY", "CONTENT_MGMT", "CONTENT_BANNER", "REVIEW_MGMT", "DATA_MGMT", "DATA_VIEW", "DASHBOARD_VIEW", "MESSAGE_MGMT");
            seedRolePermissions(3, "USER_MGMT", "USER_VIEW", "ORDER_MGMT", "ORDER_VIEW", "ORDER_REMIND_SHIP", "ORDER_REFUND", "AFTER_SALE_MGMT", "AFTER_SALE_HANDLE", "DISPUTE_MGMT", "DISPUTE_HANDLE", "CHAT_MGMT", "DASHBOARD_VIEW", "MESSAGE_MGMT");
            seedRolePermissions(4, "DATA_MGMT", "DATA_VIEW", "REPORT_VIEW", "LOG_VIEW", "USER_MGMT", "USER_VIEW", "MERCHANT_MGMT", "MERCHANT_VIEW", "GOODS_MGMT", "GOODS_VIEW", "ORDER_MGMT", "ORDER_VIEW", "DISPUTE_MGMT", "AFTER_SALE_MGMT", "DASHBOARD_VIEW");
            seedRolePermissions(5, "USER_MGMT", "USER_VIEW", "USER_RISK", "MERCHANT_MGMT", "MERCHANT_VIEW", "MERCHANT_FREEZE", "MERCHANT_CREDIT", "GOODS_MGMT", "GOODS_VIEW", "GOODS_RISK", "ORDER_MGMT", "ORDER_VIEW", "ORDER_ABNORMAL", "DISPUTE_MGMT", "DISPUTE_HANDLE", "AFTER_SALE_MGMT", "DASHBOARD_VIEW");
            seedRolePermissions(6, "ORDER_MGMT", "ORDER_VIEW", "ORDER_REFUND", "AFTER_SALE_MGMT", "AFTER_SALE_HANDLE", "FINANCE_MGMT", "FINANCE_VIEW", "FINANCE_RECONCILE", "DATA_MGMT", "DATA_VIEW", "REPORT_VIEW", "DASHBOARD_VIEW");
            seedRolePermissions(7, "GOODS_MGMT", "GOODS_VIEW", "GOODS_AUDIT", "MERCHANT_MGMT", "MERCHANT_VIEW", "CONTENT_MGMT", "CONTENT_BANNER", "REVIEW_MGMT", "DASHBOARD_VIEW");
            log.info("角色权限基础数据初始化完成");
        } catch (Exception e) {
            log.warn("角色权限基础数据初始化异常: {}", e.getMessage());
        }
    }

    private void seedRolePermissions(long roleId, String... permissionCodes) {
        for (String code : permissionCodes) {
            try {
                jdbcTemplate.update("INSERT IGNORE INTO `tb_role_permission` (`role_id`, `permission_id`) SELECT ?, `permission_id` FROM `tb_permission` WHERE `permission_code` = ?", roleId, code);
            } catch (Exception ignored) {}
        }
    }

    private void initChatBotLogTable() {

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `tb_chat_bot_log` (" +
                    "`log_id` bigint NOT NULL AUTO_INCREMENT," +
                    "`admin_id` bigint DEFAULT NULL COMMENT '管理员ID'," +
                    "`question` varchar(1000) NOT NULL COMMENT '问题'," +
                    "`answer` text NOT NULL COMMENT '回答'," +
                    "`category` varchar(50) DEFAULT NULL COMMENT '分类'," +
                    "`helpful` tinyint DEFAULT NULL COMMENT '是否有帮助 1-是 0-否'," +
                    "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (`log_id`)," +
                    "KEY `idx_admin_id` (`admin_id`)," +
                    "KEY `idx_create_time` (`create_time`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
            log.info("AI助手日志表初始化完成");
        } catch (Exception e) {
            log.warn("AI助手日志表初始化异常: {}", e.getMessage());
        }
    }

    private void initBannerData() {
        try {
            // 确保表结构完整，补全新增字段
            try { jdbcTemplate.execute("ALTER TABLE `tb_banner` MODIFY COLUMN `display_position` tinyint NOT NULL DEFAULT '1' COMMENT '展示位置：1-首页顶部 2-首页中部 3-活动页 4-分类页'"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("ALTER TABLE `tb_banner` MODIFY COLUMN `jump_type` tinyint DEFAULT NULL COMMENT '跳转类型：1-商品详情 2-分类页面 3-外部链接 4-活动页面 5-无跳转'"); } catch (Exception ignored) {}
            try { jdbcTemplate.execute("ALTER TABLE `tb_banner` MODIFY COLUMN `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-启用 2-待上架'"); } catch (Exception ignored) {}

            // 检查是否已有种子数据
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `tb_banner`", Integer.class);
            if (count != null && count > 0) {
                log.info("轮播图数据已存在，跳过种子数据初始化");
                return;
            }

            // 使用 picsum.photos 提供可访问的示例图片
            Object[][] banners = {
                    // 首页顶部轮播 (display_position=1)
                    {1, "618年中大促 全场低至3折", "https://picsum.photos/seed/banner618/1200/400", 3, "https://m.allmart.com/activity/618", 1, 1, "2026-06-01 00:00:00", "2026-06-20 23:59:59"},
                    {2, "新人专享 首单立减50元", "https://picsum.photos/seed/banner-new/1200/400", 5, null, 1, 2, null, null},
                    {3, "品牌闪购日 限时特惠", "https://picsum.photos/seed/banner-brand/1200/400", 4, "https://m.allmart.com/activity/flash", 1, 3, null, null},
                    {4, "超级会员日 专享9折起", "https://picsum.photos/seed/banner-vip/1200/400", 5, null, 1, 4, null, null},
                    {5, "周末好物精选 满减不停", "https://picsum.photos/seed/banner-weekend/1200/400", 1, "101", 1, 5, null, null},
                    // 首页中部轮播 (display_position=2)
                    {6, "数码家电焕新季", "https://picsum.photos/seed/banner-digital/1200/300", 2, "3", 2, 1, null, null},
                    {7, "美妆护肤满199减100", "https://picsum.photos/seed/banner-beauty/1200/300", 2, "5", 2, 2, null, null},
                    // 活动页轮播 (display_position=3)
                    {8, "夏日清凉节 冰爽特惠", "https://picsum.photos/seed/banner-summer/1200/400", 3, "https://m.allmart.com/activity/summer", 3, 1, "2026-06-15 00:00:00", "2026-08-31 23:59:59"},
                    // 分类页轮播 (display_position=4) - 禁用状态示例
                    {9, "食品生鲜专场 满99减20", "https://picsum.photos/seed/banner-food/1200/300", 2, "7", 4, 1, null, null},
            };
            for (Object[] b : banners) {
                jdbcTemplate.update(
                        "INSERT IGNORE INTO `tb_banner` (`banner_id`,`banner_title`,`image_url`,`jump_type`,`jump_value`,`display_position`,`sort_no`,`status`,`start_time`,`end_time`) VALUES (?,?,?,?,?,?,?,1,?,?)",
                        b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8]);
            }
            log.info("轮播图种子数据初始化完成");
        } catch (Exception e) {
            log.warn("轮播图种子数据初始化异常: {}", e.getMessage());
        }
    }
}
