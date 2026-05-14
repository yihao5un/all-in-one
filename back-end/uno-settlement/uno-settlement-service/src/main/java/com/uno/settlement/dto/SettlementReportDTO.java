package com.uno.settlement.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class SettlementReportDTO {
    private long totalBillCount;
    private long pendingBillCount;
    private long paidBillCount;
    private BigDecimal totalAmount;
    private Map<String, Long> billTypeCount;
    private List<String> recentBillNos;
}
