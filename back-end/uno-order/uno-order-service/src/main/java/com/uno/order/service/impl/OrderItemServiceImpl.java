package com.uno.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.order.entity.OrderItem;
import com.uno.order.mapper.OrderItemMapper;
import com.uno.order.service.OrderItemService;
import org.springframework.stereotype.Service;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}
