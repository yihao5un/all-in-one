package com.uno.settlement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_bill")
@Schema(description = "账单实体")
public class Bill {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "账单号")
    private String billNo;
    
    @Schema(description = "订单号")
    private String orderNo;
    
    @Schema(description = "员工ID")
    private Long employeeId;
    
    @Schema(description = "金额")
    private BigDecimal amount;
    
    @Schema(description = "账单类型 (如 ONBOARD_FEE:入职服务费)")
    private String billType;
    
    @Schema(description = "状态 (PENDING:待支付, PAID:已支付)")
    private String status;
    
    @Schema(description = "备注")
    private String remark;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
