package com.uno.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.common.exception.UnoException;
import com.uno.common.lock.DistributedLock;
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
    @DistributedLock(key = "'onboard:' + #employeeId", waitTime = 5, leaseTime = 30)
    @GlobalTransactional(name = "uno-onboard-flow", rollbackFor = Exception.class)
    public String onboard(Long employeeId, Long productId, String orderNo) {
        log.info("【入职全链路】开始执行: EmployeeID={}, ProductID={}, OrderNo={}", employeeId, productId, orderNo);
        if (employeeId == null || productId == null || orderNo == null || orderNo.isBlank()) {
            throw new UnoException("员工、产品和订单号不能为空", ResultCodeEnum.PARAM_ERROR.getCode());
        }
        
        // 1. 业务层幂等：检查该员工是否已有订单，防止 Redis 幂等失效后的漏网之鱼。
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getEmployeeId, employeeId));
        if (count > 0) {
            log.warn("[业务幂等] 发现重复入职申请, EmployeeId: {}", employeeId);
            throw new UnoException("该员工已入职，请勿重复操作", ResultCodeEnum.FAIL.getCode());
        }

        // 2. 创建订单。状态先落 CREATED，后续通过状态机推进。
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setEmployeeId(employeeId);
        order.setProductId(productId);
        order.setOrderType("ONBOARD");
        order.setStatus("CREATED");
        order.setRemark("入职调派订单：等待产品名额扣减");
        this.save(order);
        log.info("步骤1: 订单已创建 - {}", orderNo);

        // 3. 订单进入处理中，并在同一个 Seata 全局事务中扣减产品名额。
        order.setStatus("PROCESSING");
        order.setRemark("入职调派订单：正在扣减产品名额");
        this.updateById(order);
        log.info("步骤2: 订单进入 PROCESSING，准备通过 Feign 调用产品中心扣减名额...");
        com.uno.common.result.Result<Object> deductResult = productFeignClient.deduct(productId, 1);
        if (deductResult.getCode() != 200) {
            throw new com.uno.common.exception.UnoException(deductResult.getMessage(), deductResult.getCode());
        }

        // 模拟异常：使用 equals 比较对象数值，避免 == 的缓存坑
        if (Long.valueOf(999).equals(productId)) {
            log.error("触发模拟异常，准备全局回滚！");
            throw new UnoException("【模拟异常】Seata 应该让刚才创建的订单也消失！");
        }

        // 4. 核心强一致链路完成，订单进入待支付状态。RocketMQ commit 后由结算中心异步生成账单。
        order.setStatus("PENDING_PAYMENT");
        order.setRemark("入职调派订单：产品名额已扣减，等待账单支付");
        this.updateById(order);
        log.info("【入职全链路】执行成功！订单与产品名额已通过 Seata AT 保持一致。");
        return orderNo;
    }

    @Override
    public boolean existsByOrderNo(String orderNo) {
        return this.count(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)) > 0;
    }

    @Override
    public boolean existsByEmployeeId(Long employeeId) {
        return this.count(new LambdaQueryWrapper<Order>().eq(Order::getEmployeeId, employeeId)) > 0;
    }

    @Override
    public Order markSettled(String orderNo) {
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new UnoException("订单不存在", ResultCodeEnum.FAIL.getCode());
        }
        order.setStatus("SETTLED");
        order.setRemark("入职调派订单：账单已支付，订单已结算");
        this.updateById(order);
        return order;
    }

    @Override
    public Order advanceStatus(String orderNo) {
        log.info("【人力资源系统-订单中心】正在处理订单流转: {}", orderNo);
        
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new UnoException("订单不存在", ResultCodeEnum.FAIL.getCode());
        }

        String currentStatus = order.getStatus();
        
        // 简易状态机流转设计：CREATED -> PROCESSING -> PENDING_PAYMENT -> SETTLED -> CLOSED
        // 真实面试时，这里可以说使用的是状态模式(State Pattern)或者是 Spring Statemachine
        if ("CREATED".equals(currentStatus)) {
            order.setStatus("PROCESSING");
        } else if ("PROCESSING".equals(currentStatus)) {
            order.setStatus("PENDING_PAYMENT");
        } else if ("PENDING_PAYMENT".equals(currentStatus)) {
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
