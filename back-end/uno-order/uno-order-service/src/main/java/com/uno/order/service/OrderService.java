package com.uno.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
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
     * @return 订单号
     */
    String onboard(Long employeeId, Long productId);
}
