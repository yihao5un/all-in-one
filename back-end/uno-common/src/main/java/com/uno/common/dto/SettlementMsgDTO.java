package com.uno.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 结算中心接收的消息实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementMsgDTO implements Serializable {

    @JsonProperty("orderNo")
    private String orderNo;     // 订单号
    private Long employeeId;    // 员工ID
    private Long productId;     // 产品ID (新增)
    private String type;        // 业务类型: ONBOARD, BONUS, etc.
}
