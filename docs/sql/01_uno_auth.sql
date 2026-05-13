CREATE DATABASE IF NOT EXISTS `uno_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

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

-- 插入一条超级管理员测试数据
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`, `role`) VALUES (1001, 'admin', '123456', '超级管理员', 1, 'ADMIN');
