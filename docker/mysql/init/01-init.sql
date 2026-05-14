-- Business demo schema used by the current services.
-- Keep this file aligned with docs/sql/01_uno_auth.sql through docs/sql/05_seata_undo_log.sql.

CREATE DATABASE IF NOT EXISTS `uno_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `uno_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `uno_product` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `uno_settlement` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `uno_auth`;

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `status` tinyint DEFAULT '1' COMMENT '状态 1:正常 0:禁用',
  `role` varchar(20) DEFAULT 'EMPLOYEE' COMMENT '角色 ADMIN/EMPLOYEE',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人力资源系统-用户表';

INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`, `role`)
VALUES (1001, 'admin', '123456', '超级管理员', 1, 'ADMIN');

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` bigint NOT NULL,
  `xid` varchar(128) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int NOT NULL,
  `log_created` datetime(6) NOT NULL,
  `log_modified` datetime(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE `uno_order`;

CREATE TABLE IF NOT EXISTS `t_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '订单流水号 (例如: ORD20240510xxx)',
  `employee_id` bigint NOT NULL COMMENT '员工ID (关联 sys_user 表)',
  `product_id` bigint DEFAULT NULL COMMENT '关联产品/福利ID',
  `order_type` varchar(20) NOT NULL COMMENT '订单类型: ONBOARD(入职), TRANSFER(调岗), RESIGN(离职)',
  `status` varchar(30) NOT NULL COMMENT '状态机: CREATED, PROCESSING, WAIT_EXTERNAL_SYNC, PENDING_PAYMENT, SETTLED, CLOSED, SYNC_FAILED',
  `third_sync_status` varchar(30) NOT NULL DEFAULT 'NOT_SYNCED' COMMENT '第三方同步状态: NOT_SYNCED, SYNCING, SUCCESS, FAILED',
  `third_request_id` varchar(64) DEFAULT NULL COMMENT '第三方请求流水号',
  `third_response_code` varchar(32) DEFAULT NULL COMMENT '第三方响应码',
  `third_sync_time` datetime DEFAULT NULL COMMENT '第三方同步成功时间',
  `third_sync_msg` varchar(255) DEFAULT NULL COMMENT '第三方同步结果信息',
  `third_retry_count` int NOT NULL DEFAULT 0 COMMENT '第三方同步重试次数',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人力资源系统-员工调派订单表';

INSERT IGNORE INTO `t_order` (`id`, `order_no`, `employee_id`, `product_id`, `order_type`, `status`, `third_sync_status`, `remark`)
VALUES (1001, 'ORD2026051000001', 1001, 101, 'ONBOARD', 'CREATED', 'NOT_SYNCED', '超级管理员测试入职订单');

CREATE TABLE IF NOT EXISTS `t_order_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_no` varchar(64) NOT NULL COMMENT '业务单号，如订单号',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `topic` varchar(128) NOT NULL COMMENT '目标 Topic',
  `message_key` varchar(128) NOT NULL COMMENT '消息 Key',
  `payload` text NOT NULL COMMENT '消息 JSON',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PROCESSING, SENT, FAILED',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
  `sent_time` datetime DEFAULT NULL COMMENT '发送成功时间',
  `last_error` varchar(500) DEFAULT NULL COMMENT '最近一次失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_event` (`biz_no`, `event_type`),
  KEY `idx_status_retry_time` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单中心本地消息表';

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` bigint NOT NULL,
  `xid` varchar(128) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int NOT NULL,
  `log_created` datetime(6) NOT NULL,
  `log_modified` datetime(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE `uno_product`;

CREATE TABLE IF NOT EXISTS `t_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_name` varchar(100) NOT NULL COMMENT '产品/福利名称 (如: 补充公积金, 意外险)',
  `total_quota` int NOT NULL DEFAULT 0 COMMENT '总名额/额度',
  `used_quota` int NOT NULL DEFAULT 0 COMMENT '已使用名额',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='产品/福利中心表';

INSERT IGNORE INTO `t_product` (`id`, `product_name`, `total_quota`, `used_quota`) VALUES (101, '高端医疗险', 100, 5);
INSERT IGNORE INTO `t_product` (`id`, `product_name`, `total_quota`, `used_quota`) VALUES (102, '年度体检包', 500, 20);

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` bigint NOT NULL,
  `xid` varchar(128) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int NOT NULL,
  `log_created` datetime(6) NOT NULL,
  `log_modified` datetime(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='薪资结算中心-账单表';

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` bigint NOT NULL,
  `xid` varchar(128) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int NOT NULL,
  `log_created` datetime(6) NOT NULL,
  `log_modified` datetime(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
