CREATE DATABASE IF NOT EXISTS `db_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `db_product` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `db_settlement` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ==========================================================
-- 1. db_order
-- ==========================================================
USE `db_order`;

CREATE TABLE IF NOT EXISTS t_company (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    company_name    VARCHAR(200) NOT NULL COMMENT '企业名称',
    short_name      VARCHAR(50)  COMMENT '企业简称',
    contact_name    VARCHAR(50)  COMMENT '联系人',
    contact_phone   VARCHAR(20)  COMMENT '联系电话',
    contact_email   VARCHAR(100) COMMENT '联系邮箱',
    region          VARCHAR(20)  NOT NULL COMMENT '所属地区(CN/ID/SG等)',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1-启用 0-停用',
    created_by      VARCHAR(50),
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    KEY idx_region (region),
    KEY idx_status (status)
) COMMENT='企业客户表';

CREATE TABLE IF NOT EXISTS t_employee (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    employee_no     VARCHAR(32)  NOT NULL COMMENT '员工编号',
    company_id      BIGINT       NOT NULL COMMENT '所属企业ID(分片键)',
    name            VARCHAR(50)  NOT NULL COMMENT '姓名',
    id_card         VARCHAR(30)  COMMENT '身份证号',
    phone           VARCHAR(20)  COMMENT '手机号',
    email           VARCHAR(100) COMMENT '邮箱',
    department      VARCHAR(100) COMMENT '部门',
    position        VARCHAR(100) COMMENT '职位',
    entry_date      DATE         COMMENT '入职日期',
    leave_date      DATE         COMMENT '离职日期',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1-在职 2-离职 3-待入职',
    base_salary     DECIMAL(12,2) COMMENT '基本工资',
    created_by      VARCHAR(50),
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_employee_no (employee_no),
    KEY idx_company_id (company_id),
    KEY idx_company_status (company_id, status),
    KEY idx_name (name)
) COMMENT='员工表(累计200万+行)';

CREATE TABLE IF NOT EXISTS t_order (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(32)  NOT NULL COMMENT '订单编号(雪花算法,含company基因)',
    company_id      BIGINT       NOT NULL COMMENT '企业ID(分片键)',
    employee_id     BIGINT       NOT NULL COMMENT '员工ID',
    order_type      VARCHAR(20)  NOT NULL COMMENT 'ONBOARD-入职 TRANSFER-调派 RESIGN-离职 SALARY_ADJUST-调薪',
    status          VARCHAR(20)  NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PROCESSING/COMPLETED/CANCELLED/FAILED',
    product_id      BIGINT       COMMENT '关联产品ID',
    effective_date  DATE         COMMENT '生效日期',
    remark          VARCHAR(500) COMMENT '备注',
    created_by      VARCHAR(50),
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(50),
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    version         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_company_id (company_id),
    KEY idx_employee_id (employee_id),
    KEY idx_company_status (company_id, status),
    KEY idx_created_time (created_time)
) COMMENT='调派订单表';

CREATE TABLE IF NOT EXISTS t_local_message (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    msg_id          VARCHAR(64)  NOT NULL COMMENT '消息唯一ID',
    biz_type        VARCHAR(30)  NOT NULL COMMENT '业务类型(SETTLEMENT/NOTIFY)',
    biz_key         VARCHAR(64)  NOT NULL COMMENT '业务键(order_no)',
    topic           VARCHAR(100) NOT NULL COMMENT 'MQ Topic',
    msg_body        TEXT         NOT NULL COMMENT '消息体JSON',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待发送 1-已发送 2-失败',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    max_retry       INT          NOT NULL DEFAULT 5,
    next_retry_time DATETIME     COMMENT '下次重试时间',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_status_retry (status, next_retry_time)
) COMMENT='本地消息表';

CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT       NOT NULL COMMENT 'branch transaction id',
    xid           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    rollback_info LONGBLOB     NOT NULL COMMENT 'rollback info',
    log_status    INT          NOT NULL COMMENT '0:normal status,1:defense status',
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) COMMENT='Seata AT模式undo_log';

-- ==========================================================
-- 2. db_product
-- ==========================================================
USE `db_product`;

CREATE TABLE IF NOT EXISTS t_product (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    product_code    VARCHAR(32)  NOT NULL COMMENT '产品编码',
    product_name    VARCHAR(100) NOT NULL COMMENT '产品名称',
    category        VARCHAR(30)  NOT NULL COMMENT 'SOCIAL_SECURITY/HOUSING_FUND/PAYROLL/TAX',
    region          VARCHAR(20)  NOT NULL COMMENT '适用地区',
    unit_price      DECIMAL(12,2) COMMENT '单价(服务费/人/月)',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1-上架 0-下架',
    description     VARCHAR(500) COMMENT '产品描述',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_category (category),
    KEY idx_region (region)
) COMMENT='产品表';

CREATE TABLE IF NOT EXISTS t_product_quota (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    company_id      BIGINT       NOT NULL COMMENT '企业ID',
    product_id      BIGINT       NOT NULL COMMENT '产品ID',
    total_quota     INT          NOT NULL COMMENT '总名额',
    used_quota      INT          NOT NULL DEFAULT 0 COMMENT '已用名额',
    remaining_quota INT          NOT NULL COMMENT '剩余名额',
    effective_start DATE         NOT NULL COMMENT '合同生效起始',
    effective_end   DATE         NOT NULL COMMENT '合同生效截止',
    version         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_company_product (company_id, product_id),
    KEY idx_company_id (company_id)
) COMMENT='产品名额表';

CREATE TABLE IF NOT EXISTS t_rate_rule (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    product_id      BIGINT       NOT NULL COMMENT '产品ID',
    region          VARCHAR(20)  NOT NULL COMMENT '地区',
    rule_type       VARCHAR(30)  NOT NULL COMMENT 'SOCIAL_BASE/TAX_RATE/HOUSING_RATE',
    rule_name       VARCHAR(100) NOT NULL COMMENT '规则名称',
    rate_value      DECIMAL(8,4) COMMENT '费率值',
    min_base        DECIMAL(12,2) COMMENT '最低基数',
    max_base        DECIMAL(12,2) COMMENT '最高基数',
    effective_year  INT          NOT NULL COMMENT '生效年份',
    status          TINYINT      NOT NULL DEFAULT 1,
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_product_region (product_id, region),
    KEY idx_year (effective_year)
) COMMENT='费率规则表';

CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT       NOT NULL COMMENT 'branch transaction id',
    xid           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    rollback_info LONGBLOB     NOT NULL COMMENT 'rollback info',
    log_status    INT          NOT NULL COMMENT '0:normal status,1:defense status',
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) COMMENT='Seata AT模式undo_log';

-- ==========================================================
-- 3. db_settlement
-- ==========================================================
USE `db_settlement`;

CREATE TABLE IF NOT EXISTS t_salary_detail (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    company_id      BIGINT       NOT NULL COMMENT '企业ID(分片键)',
    employee_id     BIGINT       NOT NULL COMMENT '员工ID',
    settle_month    VARCHAR(7)   NOT NULL COMMENT '结算月份(2026-05)',
    base_salary     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '基本工资',
    social_security DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '社保(企业+个人)',
    housing_fund    DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '公积金',
    tax_amount      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '个税',
    supplement      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '补发金额',
    deduction       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '扣减金额',
    net_salary      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '实发工资',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CALCULATING/SETTLED/PAID',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_employee_month (employee_id, settle_month),
    KEY idx_company_month (company_id, settle_month),
    KEY idx_settle_month (settle_month),
    KEY idx_status (status)
) COMMENT='薪资明细表';

CREATE TABLE IF NOT EXISTS t_salary_detail_history LIKE t_salary_detail;

CREATE TABLE IF NOT EXISTS t_bill (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    bill_no         VARCHAR(32)  NOT NULL COMMENT '账单编号',
    company_id      BIGINT       NOT NULL COMMENT '企业ID(分片键)',
    order_id        BIGINT       COMMENT '关联订单ID',
    settle_month    VARCHAR(7)   NOT NULL COMMENT '结算月份',
    bill_type       VARCHAR(20)  NOT NULL COMMENT 'SERVICE_FEE/SALARY/SOCIAL_SECURITY',
    amount          DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '账单金额',
    status          VARCHAR(20)  NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/CONFIRMED/INVOICED/PAID',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_month (order_id, settle_month),
    KEY idx_company_month (company_id, settle_month),
    KEY idx_status (status)
) COMMENT='账单表';

CREATE TABLE IF NOT EXISTS t_charge (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    charge_no       VARCHAR(32)  NOT NULL COMMENT '收费编号',
    company_id      BIGINT       NOT NULL COMMENT '企业ID',
    bill_id         BIGINT       NOT NULL COMMENT '关联账单ID',
    charge_type     VARCHAR(20)  NOT NULL COMMENT 'SERVICE_FEE/SOCIAL_SECURITY/HOUSING_FUND',
    amount          DECIMAL(14,2) NOT NULL COMMENT '收费金额',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CHARGED/FAILED',
    charged_time    DATETIME     COMMENT '收费时间',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_charge_no (charge_no),
    KEY idx_company_id (company_id),
    KEY idx_bill_id (bill_id)
) COMMENT='收费记录表';

CREATE TABLE IF NOT EXISTS t_invoice (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    invoice_no      VARCHAR(32)  NOT NULL COMMENT '发票编号',
    company_id      BIGINT       NOT NULL COMMENT '企业ID',
    charge_id       BIGINT       NOT NULL COMMENT '关联收费ID',
    invoice_type    VARCHAR(20)  NOT NULL COMMENT 'NORMAL-普票 SPECIAL-专票',
    amount          DECIMAL(14,2) NOT NULL COMMENT '开票金额',
    tax_rate        DECIMAL(5,2) NOT NULL DEFAULT 6.00 COMMENT '税率%',
    tax_amount      DECIMAL(14,2) NOT NULL COMMENT '税额',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    external_invoice_id VARCHAR(64) COMMENT '三方开票系统返回的流水号',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invoice_charge (charge_id, invoice_type),
    UNIQUE KEY uk_invoice_no (invoice_no),
    KEY idx_company_id (company_id),
    KEY idx_status (status)
) COMMENT='开票表';

CREATE TABLE IF NOT EXISTS t_idempotent (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    biz_type        VARCHAR(30)  NOT NULL COMMENT '业务类型',
    biz_key         VARCHAR(128) NOT NULL COMMENT '业务唯一键',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    result          TEXT         COMMENT '处理结果JSON',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_idempotent_key (biz_type, biz_key)
) COMMENT='幂等表';
