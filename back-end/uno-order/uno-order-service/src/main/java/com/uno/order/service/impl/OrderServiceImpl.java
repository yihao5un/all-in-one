package com.uno.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.common.exception.UnoException;
import com.uno.common.result.ResultCodeEnum;
import com.uno.order.entity.Order;
import com.uno.order.mapper.OrderMapper;
import com.uno.order.service.OrderService;
import com.uno.product.api.ProductFeignClient;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    @GlobalTransactional(name = "uno-onboard-flow", rollbackFor = Exception.class)
    public String onboard(Long employeeId, Long productId, String orderNo) {
        log.info("🚀 【入职全链路】开始执行: EmployeeID={}, ProductID={}", employeeId, productId);
        
        // 步骤1: 创建订单 (直接使用传入的订单号)
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setEmployeeId(employeeId);
        order.setOrderType("ONBOARD");
        order.setStatus("CREATED");
        order.setRemark("Seata 分布式事务测试入职单");
        this.save(order);
        log.info("✅ 步骤1: 订单已创建 - {}", orderNo);

        // 2. 扣减福利名额 (远程调用 - 另一个微服务，另一个数据库)
        log.info("➡️ 步骤2: 正在通过 Feign 调用产品中心扣减名额...");
        productFeignClient.deduct(productId, 1);

        // 模拟异常：使用 equals 比较对象数值，避免 == 的缓存坑
        if (Long.valueOf(999).equals(productId)) {
            log.error("❌ 触发模拟异常，准备全局回滚！");
            throw new UnoException("【模拟异常】Seata 应该让刚才创建的订单也消失！");
        }

        log.info("🎉 【入职全链路】执行成功！分布式事务已提交。");
        return orderNo;
    }

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
