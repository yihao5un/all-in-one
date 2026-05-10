package com.uno.settlement.controller;

import com.uno.common.result.Result;
import com.uno.settlement.service.BillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settlement")
public class BillController {

    @Autowired
    private BillService billService;

    @PostMapping("/create")
    public Result<Object> create(@RequestParam("orderNo") String orderNo, @RequestParam("employeeId") Long employeeId) {
        billService.createBill(orderNo, employeeId);
        return Result.success(null);
    }
}
