CREATE TABLE IF NOT EXISTS tb_goods_stock_log (
  log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  goods_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  old_stock INT NOT NULL,
  new_stock INT NOT NULL,
  change_stock INT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_merchant_time (merchant_id, create_time),
  KEY idx_sku_time (sku_id, create_time),
  KEY idx_goods_time (goods_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

