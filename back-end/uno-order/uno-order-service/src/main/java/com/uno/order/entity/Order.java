package com.uno.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String orderNo;
    
    private Long employeeId;

    private Long productId;
    
    private String orderType;
    
    private String status;

    private String thirdSyncStatus;

    private String thirdRequestId;

    private String thirdResponseCode;

    private LocalDateTime thirdSyncTime;

    private String thirdSyncMsg;

    private Integer thirdRetryCount;
    
    private String remark;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
