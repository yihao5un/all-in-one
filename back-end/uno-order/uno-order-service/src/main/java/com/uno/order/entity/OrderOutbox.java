package com.uno.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_outbox")
@Schema(description = "订单事件外发盒实体 (Outbox)")
public class OrderOutbox {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "业务单号")
    private String bizNo;
    
    @Schema(description = "事件类型")
    private String eventType;
    
    @Schema(description = "消息队列 Topic")
    private String topic;
    
    @Schema(description = "消息 Key")
    private String messageKey;
    
    @Schema(description = "消息载荷 (JSON)")
    private String payload;
    
    @Schema(description = "状态 (PENDING, SENT, FAILED)")
    private String status;
    
    @Schema(description = "重试次数")
    private Integer retryCount;
    
    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;
    
    @Schema(description = "发送时间")
    private LocalDateTime sentTime;
    
    @Schema(description = "最后一次错误信息")
    private String lastError;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
