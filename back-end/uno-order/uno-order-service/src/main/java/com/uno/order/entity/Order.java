package com.uno.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_order")
@Schema(description = "订单实体")
public class Order {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "订单号")
    private String orderNo;
    
    @Schema(description = "员工ID")
    private Long employeeId;

    @Schema(description = "产品ID")
    private Long productId;
    
    @Schema(description = "订单类型 (如 ONBOARD:入职)")
    private String orderType;
    
    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "第三方同步状态")
    private String thirdSyncStatus;

    @Schema(description = "第三方请求流水号")
    private String thirdRequestId;

    @Schema(description = "第三方响应码")
    private String thirdResponseCode;

    @Schema(description = "第三方同步成功时间")
    private LocalDateTime thirdSyncTime;

    @Schema(description = "第三方同步结果信息")
    private String thirdSyncMsg;

    @Schema(description = "第三方同步重试次数")
    private Integer thirdRetryCount;
    
    @Schema(description = "备注")
    private String remark;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
