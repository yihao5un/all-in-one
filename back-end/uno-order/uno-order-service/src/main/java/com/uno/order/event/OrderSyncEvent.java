package com.uno.order.event;

import org.springframework.context.ApplicationEvent;

public class OrderSyncEvent extends ApplicationEvent {
    
    private final String orderNo;

    public OrderSyncEvent(Object source, String orderNo) {
        super(source);
        this.orderNo = orderNo;
    }

    public String getOrderNo() {
        return orderNo;
    }
}
