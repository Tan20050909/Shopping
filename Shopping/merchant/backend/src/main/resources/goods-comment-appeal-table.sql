CREATE TABLE IF NOT EXISTS tb_goods_comment_appeal (
  appeal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  comment_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  appeal_reason VARCHAR(500) NOT NULL,
  appeal_evidence TEXT NULL,
  appeal_status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  handler_id BIGINT NULL,
  handle_remark VARCHAR(200) NULL,
  handle_time DATETIME NULL,
  KEY idx_comment_id (comment_id),
  KEY idx_merchant_status (merchant_id, appeal_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

