package com.uno.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_outbox")
public class OrderOutbox {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizNo;
    private String eventType;
    private String topic;
    private String messageKey;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime sentTime;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
