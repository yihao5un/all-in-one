CREATE DATABASE IF NOT EXISTS `uno_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `uno_order`;

CREATE TABLE IF NOT EXISTS `t_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '订单流水号 (例如: ORD20240510xxx)',
  `employee_id` bigint NOT NULL COMMENT '员工ID (关联 sys_user 表)',
  `product_id` bigint DEFAULT NULL COMMENT '关联产品/福利ID',
  `order_type` varchar(20) NOT NULL COMMENT '订单类型: ONBOARD(入职), TRANSFER(调岗), RESIGN(离职)',
  `status` varchar(20) NOT NULL COMMENT '状态机: CREATED, PROCESSING, PENDING_PAYMENT, SETTLED, CLOSED',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人力资源系统-员工调派订单表';

-- 插入一条测试数据
INSERT IGNORE INTO `t_order` (`id`, `order_no`, `employee_id`, `order_type`, `status`, `remark`) 
VALUES (1001, 'ORD2026051000001', 1001, 'ONBOARD', 'CREATED', '超级管理员测试入职订单');
