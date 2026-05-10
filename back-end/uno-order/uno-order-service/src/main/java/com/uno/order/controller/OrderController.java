package com.uno.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uno.common.result.Result;
import com.uno.order.entity.Order;
import com.uno.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderNo}")
    public Result<Object> getOrder(@PathVariable("orderNo") String orderNo) {
        Order order = orderService.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return Result.fail().message("查无此订单");
        }
        return Result.success(order);
    }

    /**
     * 触发订单流转 (状态机演练)
     */
    @PostMapping("/advance")
    public Result<Object> advanceStatus(@RequestParam("orderNo") String orderNo) {
        Order updatedOrder = orderService.advanceStatus(orderNo);
        return Result.success(updatedOrder);
    }

    /**
     * 核心业务：全链路入职 (Seata 分布式事务演示)
     */
    @PostMapping("/onboard")
    public Result<Object> onboard(@RequestParam("employeeId") Long employeeId, @RequestParam("productId") Long productId) {
        String orderNo = orderService.onboard(employeeId, productId);
        return Result.success(orderNo);
    }
}
