package com.uno.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.common.exception.UnoException;
import com.uno.common.result.ResultCodeEnum;
import com.uno.order.entity.Order;
import com.uno.order.mapper.OrderMapper;
import com.uno.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Override
    public Order advanceStatus(String orderNo) {
        log.info("【人力资源系统-订单中心】正在处理订单流转: {}", orderNo);
        
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new UnoException("订单不存在", ResultCodeEnum.FAIL.getCode());
        }

        String currentStatus = order.getStatus();
        
        // 简易状态机流转设计：CREATED -> PROCESSING -> SETTLED -> CLOSED
        // 真实面试时，这里可以说使用的是状态模式(State Pattern)或者是 Spring Statemachine
        if ("CREATED".equals(currentStatus)) {
            order.setStatus("PROCESSING");
        } else if ("PROCESSING".equals(currentStatus)) {
            order.setStatus("SETTLED");
        } else if ("SETTLED".equals(currentStatus)) {
            order.setStatus("CLOSED");
        } else {
            throw new UnoException("订单已结束或状态异常，无法推进: " + currentStatus, ResultCodeEnum.FAIL.getCode());
        }

        this.updateById(order);
        log.info("🎯 订单 {} 状态流转成功: {} ➡️ {}", orderNo, currentStatus, order.getStatus());
        
        return order;
    }
}
