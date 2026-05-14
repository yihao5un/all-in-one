package com.uno.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单中心接收的外部三方同步消息。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalSyncMsgDTO implements Serializable {

    @JsonProperty("orderNo")
    private String orderNo;
    private Long employeeId;
    private Long productId;
    private String type;
}
