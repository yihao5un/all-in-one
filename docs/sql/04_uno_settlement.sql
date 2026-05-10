CREATE DATABASE IF NOT EXISTS `uno_settlement` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `uno_settlement`;

CREATE TABLE IF NOT EXISTS `t_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bill_no` varchar(32) NOT NULL COMMENT '账单编号',
  `order_no` varchar(32) NOT NULL COMMENT '关联订单号',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '结算金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '账单状态: 0-待支付, 1-已支付',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='薪资结算中心-账单表';
