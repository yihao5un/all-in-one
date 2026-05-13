package com.uno.order.dto;

import lombok.Data;

@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private Long employeeId;
    private Long productId;
    private String orderType;
    private String status;
    private String remark;
}
