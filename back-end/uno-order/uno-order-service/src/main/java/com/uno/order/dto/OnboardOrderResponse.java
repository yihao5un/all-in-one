package com.uno.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OnboardOrderResponse {
    private String orderNo;
    private String status;
}
