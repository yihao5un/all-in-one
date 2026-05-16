package com.uno.order.dto;
 
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private Long employeeId;
    private java.util.List<Long> productIds;
    private String orderType;
    private String status;
    private String thirdSyncStatus;
    private String thirdRequestId;
    private String thirdResponseCode;
    private String thirdSyncTime;
    private String thirdSyncMsg;
    private Integer thirdRetryCount;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean billSyncSent;
}
