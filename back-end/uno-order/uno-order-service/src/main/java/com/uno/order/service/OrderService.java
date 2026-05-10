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
}
