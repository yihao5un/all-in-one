package com.uno.order.dto;

import com.uno.common.dto.ProductItemDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 入职申请请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "全链路入职申请请求对象")
public class OnboardRequestDTO {

    @Schema(description = "员工ID", example = "1001")
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @Schema(description = "福利产品列表(包含数量)", example = "[{\"productId\": 2001, \"count\": 1}]")
    @NotNull(message = "产品列表不能为空")
    private List<ProductItemDTO> products;

    @Schema(description = "订单号 (后端自动生成)", accessMode = Schema.AccessMode.READ_ONLY)
    private String orderNo;

}
