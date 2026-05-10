CREATE DATABASE IF NOT EXISTS `uno_settlement` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `uno_settlement`;

CREATE TABLE IF NOT EXISTS `t_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bill_no` varchar(32) NOT NULL COMMENT '账单编号',
  `order_no` varchar(32) NOT NULL COMMENT '关联订单号',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '结算金额',
  `bill_type` varchar(50) DEFAULT NULL COMMENT '账单类型',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='薪资结算中心-账单表';
