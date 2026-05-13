package com.uno.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uno.common.lock.DistributedLock;
import com.uno.order.entity.Order;

public interface OrderService extends IService<Order> {
    
    /**
     * 核心业务：推进订单状态 (简易版状态机)
     * @param orderNo 订单号
     * @return 更新后的订单信息
     */
    Order advanceStatus(String orderNo);

    /**
     * 入职全链路业务 (演示 Seata 分布式事务)
     * @param employeeId 员工ID
     * @param productId 关联领取的福利产品ID
     * @param orderNo 订单号
     * @return 订单号
     */
    @DistributedLock(key = "'onboard:' + #employeeId", waitTime = 5, leaseTime = 30)
    String onboard(Long employeeId, Long productId, String orderNo);

    /**
     * 查询订单是否已成功落库，用于 RocketMQ 事务消息回查。
     */
    boolean existsByOrderNo(String orderNo);

    /**
     * 查询员工是否已有入职订单。
     */
    boolean existsByEmployeeId(Long employeeId);

    /**
     * 支付完成后关闭订单结算链路。
     */
    Order markSettled(String orderNo);
}
