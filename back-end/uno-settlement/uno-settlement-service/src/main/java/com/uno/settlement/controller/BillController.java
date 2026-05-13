package com.uno.settlement.controller;

import com.uno.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uno.order.api.OrderFeignClient;
import com.uno.settlement.entity.Bill;
import com.uno.settlement.service.BillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settlement")
public class BillController {

    @Autowired
    private BillService billService;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @PostMapping("/orders/{orderNo}/rebuild-bill")
    public Result<Object> rebuildBill(@PathVariable("orderNo") String orderNo) {
        return Result.success(billService.rebuildBillFromOrder(orderNo));
    }

    @GetMapping("/list")
    public Result<Object> list() {
        return Result.success(billService.list(new LambdaQueryWrapper<Bill>().orderByDesc(Bill::getCreateTime)));
    }

    @PostMapping("/{billNo}/pay")
    public Result<Object> pay(@PathVariable("billNo") String billNo) {
        Bill bill = billService.getOne(new LambdaQueryWrapper<Bill>().eq(Bill::getBillNo, billNo));
        if (bill == null) {
            return Result.fail().message("账单不存在");
        }
        bill.setStatus("PAID");
        billService.updateById(bill);
        orderFeignClient.settle(bill.getOrderNo());
        return Result.success(bill);
    }

    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable("id") Long id) {
        billService.removeById(id);
        return Result.success();
    }
}
