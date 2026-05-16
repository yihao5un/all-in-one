package com.uno.order.external;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import com.uno.common.dto.ProductItemDTO;

@Data
@AllArgsConstructor
public class ExternalSyncRequest {
    private String requestId;
    private String orderNo;
    private Long employeeId;
    private List<ProductItemDTO> products;
    private String type;
}
