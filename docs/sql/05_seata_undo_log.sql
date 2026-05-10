-- 在 uno_order 库中创建 undo_log 表
USE `uno_order`;
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

-- 在 uno_auth 库中创建 undo_log 表
USE `uno_auth`;
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


-- 在 uno_product 库中创建 undo_log 表
USE `uno_product`;
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

-- 在 uno_settlement 库中创建 undo_log 表
USE `uno_settlement`;
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

