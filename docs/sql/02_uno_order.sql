CREATE DATABASE IF NOT EXISTS `uno_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

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
