CREATE DATABASE IF NOT EXISTS `uno_product` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

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

-- 初始化两个人力资源产品
INSERT IGNORE INTO `t_product` (`id`, `product_name`, `total_quota`, `used_quota`) VALUES (101, '高端医疗险', 100, 5);
INSERT IGNORE INTO `t_product` (`id`, `product_name`, `total_quota`, `used_quota`) VALUES (102, '年度体检包', 500, 20);
