package com.uno.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_bill")
public class Bill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String billNo;
    private String orderNo;
    private Long employeeId;
    private BigDecimal amount;
    private String billType;    // 账单类型
    private String status;      // 状态: PENDING, PAID, etc.
    private String remark;      // 备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
