package com.uno.order.external;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExternalSyncRequest {
    private String requestId;
    private String orderNo;
    private Long employeeId;
    private Long productId;
    private String type;
}
