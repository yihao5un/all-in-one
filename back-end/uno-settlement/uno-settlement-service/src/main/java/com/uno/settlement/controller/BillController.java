package com.uno.settlement.controller;

import com.uno.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uno.settlement.entity.Bill;
import com.uno.settlement.service.BillService;
import com.uno.settlement.service.SettlementReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settlement")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    private final SettlementReportService settlementReportService;

    @PostMapping("/orders/{orderNo}/rebuild-bill")
    public Result<Object> rebuildBill(@PathVariable("orderNo") String orderNo) {
        return Result.success(billService.rebuildBillFromOrder(orderNo));
    }

    @GetMapping("/list")
    public Result<Object> list() {
        return Result.success(billService.list(new LambdaQueryWrapper<Bill>().orderByDesc(Bill::getCreateTime)));
    }

    @GetMapping("/report/summary")
    public Result<Object> summaryReport() {
        return Result.success(settlementReportService.buildSummaryReport());
    }

    @PostMapping("/{billNo}/pay")
    public Result<Object> pay(@PathVariable("billNo") String billNo) {
        return Result.success(billService.payBill(billNo));
    }

    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable("id") Long id) {
        billService.removeById(id);
        return Result.success();
    }
}
