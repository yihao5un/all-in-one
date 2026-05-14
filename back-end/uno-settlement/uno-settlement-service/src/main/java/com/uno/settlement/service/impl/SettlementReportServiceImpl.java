package com.uno.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uno.settlement.dto.SettlementReportDTO;
import com.uno.settlement.entity.Bill;
import com.uno.settlement.service.BillService;
import com.uno.settlement.service.SettlementReportService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class SettlementReportServiceImpl implements SettlementReportService {

    private final BillService billService;
    private final Executor reportExecutor;

    public SettlementReportServiceImpl(BillService billService,
                                       @Qualifier("settlementReportExecutor") Executor reportExecutor) {
        this.billService = billService;
        this.reportExecutor = reportExecutor;
    }

    @Override
    public SettlementReportDTO buildSummaryReport() {
        CompletableFuture<List<Bill>> allBillsFuture = CompletableFuture.supplyAsync(billService::list, reportExecutor);
        CompletableFuture<Long> pendingCountFuture = CompletableFuture.supplyAsync(
                () -> billService.count(new LambdaQueryWrapper<Bill>().eq(Bill::getStatus, "PENDING")),
                reportExecutor
        );
        CompletableFuture<Long> paidCountFuture = CompletableFuture.supplyAsync(
                () -> billService.count(new LambdaQueryWrapper<Bill>().eq(Bill::getStatus, "PAID")),
                reportExecutor
        );

        CompletableFuture.allOf(allBillsFuture, pendingCountFuture, paidCountFuture).join();

        List<Bill> allBills = allBillsFuture.join();
        SettlementReportDTO report = new SettlementReportDTO();
        report.setTotalBillCount(allBills.size());
        report.setPendingBillCount(pendingCountFuture.join());
        report.setPaidBillCount(paidCountFuture.join());
        report.setTotalAmount(allBills.stream()
                .map(Bill::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setBillTypeCount(allBills.stream()
                .collect(Collectors.groupingBy(
                        bill -> bill.getBillType() == null ? "UNKNOWN" : bill.getBillType(),
                        Collectors.counting()
                )));
        report.setRecentBillNos(allBills.stream()
                .sorted(Comparator.comparing(Bill::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(Bill::getBillNo)
                .collect(Collectors.toList()));
        return report;
    }
}
