package com.uno.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.common.dto.SettlementMsgDTO;
import com.uno.common.exception.UnoException;
import com.uno.common.dto.ExternalSyncMsgDTO;
import com.uno.common.dto.ProductItemDTO;
import java.util.stream.Collectors;
import com.uno.common.enums.BizNoPrefixEnum;
import com.uno.common.lock.DistributedLock;
import com.uno.common.result.Result;
import com.uno.common.result.ResultCodeEnum;
import com.uno.common.util.BizNoGenerator;
import com.uno.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import com.uno.order.entity.Order;
import com.uno.order.mapper.OrderMapper;
import com.uno.order.service.OrderOutboxService;
import com.uno.order.service.OrderService;
import com.uno.order.service.OrderItemService;
import com.uno.order.dto.OnboardRequestDTO;
import com.uno.order.dto.OrderDTO;
import com.uno.order.enums.OrderStatusEnum;
import com.uno.order.enums.OrderSyncStatusEnum;
import com.uno.order.enums.OrderTypeEnum;
import com.uno.product.api.ProductFeignClient;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.uno.order.mapper.OrderItemMapper;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final ProductFeignClient productFeignClient;

    private final RocketMQTemplate rocketMQTemplate;

    private final OrderOutboxService orderOutboxService;

    private final BizNoGenerator bizNoGenerator;

    private final OrderItemService orderItemService;

    @Override
    @DistributedLock(key = "'onboard:' + #onboardRequestDTO.employeeId", waitTime = 5, leaseTime = 30)
    @GlobalTransactional(name = "uno-onboard-flow", rollbackFor = Exception.class)
    public String onboard(OnboardRequestDTO onboardRequestDTO) {
        String orderNo = bizNoGenerator.generate(BizNoPrefixEnum.ONBOARD);
        onboardRequestDTO.setOrderNo(orderNo);

        Long employeeId = onboardRequestDTO.getEmployeeId();
        List<ProductItemDTO> products = onboardRequestDTO.getProducts();

        log.info("【入职全链路】开始执行: EmployeeID={}, Products={}, OrderNo={}", employeeId, products, orderNo);
        
        // 1. 业务层幂等：检查该员工是否已有订单，防止 Redis 幂等失效。
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getEmployeeId, employeeId));
        if (count > 0) {
            log.warn("[业务幂等] 发现重复入职申请, EmployeeId: {}", employeeId);
            throw new UnoException("该员工已入职，请勿重复操作", ResultCodeEnum.FAIL.getCode());
        }

        // 2. 创建订单主表。因为紧接着就要处理，直接以 PROCESSING（处理中）状态落库，节省一次 DB 的 UPDATE 操作。
        Order order = Order.builder()
                .orderNo(orderNo)
                .employeeId(employeeId)
                .orderType(OrderTypeEnum.ONBOARD.getCode())
                .status(OrderStatusEnum.PROCESSING.getCode())
                .thirdSyncStatus(OrderSyncStatusEnum.NOT_SYNCED.getCode())
                .thirdRetryCount(0)
                .remark("入职调派订单：正在扣减产品名额")
                .build();
        this.save(order);
        
        // 2.5 批量创建订单明细表。
        List<OrderItem> orderItemList = products.stream().map(p ->
            OrderItem.builder()
                    .orderNo(orderNo)
                    .productId(p.getProductId())
                    .count(p.getCount())
                    .build()
        ).collect(Collectors.toList());
        orderItemService.saveBatch(orderItemList);
        log.info("步骤1: 订单及明细已创建并进入处理中 - {}", orderNo);

        log.info("步骤2: 准备通过 Feign 调用产品中心批量扣减名额...");
        Result<Object> deductResult = productFeignClient.deduct(products, orderNo);
        if (deductResult.getCode() != 200) {
            throw new UnoException("批量扣减产品名额失败: " + deductResult.getMessage(), deductResult.getCode());
        }

        // 模拟异常：使用 contains 比较，避免 == 的缓存坑
        List<Long> productIds = products.stream().map(ProductItemDTO::getProductId).toList();
        if (productIds.contains(999L)) {
            log.error("触发模拟异常，准备全局回滚！");
            throw new UnoException("【模拟异常】Seata 应该让刚才创建的订单也消失！");
        }

        // 4. 内部强一致链路完成，外部三方系统进入异步最终一致阶段。
        order.setStatus(OrderStatusEnum.WAIT_EXTERNAL_SYNC.getCode());
        order.setThirdSyncStatus(OrderSyncStatusEnum.NOT_SYNCED.getCode());
        order.setRemark("入职调派订单：产品名额已扣减，等待第三方系统同步");
        this.updateById(order);

        orderOutboxService.saveExternalSyncEvent(new ExternalSyncMsgDTO(orderNo, employeeId, products, OrderTypeEnum.ONBOARD.getCode()));
        log.info("【入职核心链路】执行成功！订单与产品名额已通过 Seata AT 保持一致，等待外部三方同步。");
        return orderNo;
    }

    @Override
    public OrderDTO getOrderDTO(String orderNo) {
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return null;
        }
        
        List<Long> productIds = orderItemService.list(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo)
        ).stream().map(OrderItem::getProductId).collect(Collectors.toList());

        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setEmployeeId(order.getEmployeeId());
        dto.setProductIds(productIds);
        dto.setOrderType(order.getOrderType());
        dto.setStatus(order.getStatus());
        dto.setThirdSyncStatus(order.getThirdSyncStatus());
        dto.setThirdRequestId(order.getThirdRequestId());
        dto.setThirdResponseCode(order.getThirdResponseCode());
        dto.setThirdSyncTime(order.getThirdSyncTime() == null ? null : order.getThirdSyncTime().toString());
        dto.setThirdSyncMsg(order.getThirdSyncMsg());
        dto.setThirdRetryCount(order.getThirdRetryCount());
        dto.setRemark(order.getRemark());
        
        return dto;
    }

    @Override
    public Order markExternalSyncing(String orderNo, String requestId) {
        Order order = getOrderOrThrow(orderNo);
        order.setStatus(OrderStatusEnum.WAIT_EXTERNAL_SYNC.getCode());
        order.setThirdSyncStatus(OrderSyncStatusEnum.SYNCING.getCode());
        order.setThirdRequestId(requestId);
        order.setThirdResponseCode(null);
        order.setThirdSyncMsg("第三方系统同步中");
        order.setRemark("入职调派订单：正在同步第三方外服系统");
        this.updateById(order);
        return order;
    }

    @Override
    public Order markExternalSyncSuccess(String orderNo, String requestId, String responseCode, String message) {
        Order order = getOrderOrThrow(orderNo);
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setThirdSyncStatus(OrderSyncStatusEnum.SUCCESS.getCode());
        order.setThirdRequestId(requestId);
        order.setThirdResponseCode(responseCode);
        order.setThirdSyncTime(LocalDateTime.now());
        order.setThirdSyncMsg(message);
        order.setRemark("入职调派订单：第三方同步成功，等待账单支付");
        this.updateById(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order markExternalSyncSuccessAndSaveSettlementEvent(String orderNo,
                                                               String requestId,
                                                               String responseCode,
                                                               String message,
                                                               SettlementMsgDTO settlementMsg) {
        Order order = markExternalSyncSuccess(orderNo, requestId, responseCode, message);
        orderOutboxService.saveSettlementEvent(settlementMsg);
        return order;
    }

    @Override
    public Order markExternalSyncFailed(String orderNo, String requestId, String responseCode, String message, boolean finalFailure) {
        Order order = getOrderOrThrow(orderNo);
        int retryCount = order.getThirdRetryCount() == null ? 0 : order.getThirdRetryCount();
        order.setThirdRetryCount(retryCount + 1);
        order.setThirdRequestId(requestId);
        order.setThirdResponseCode(responseCode);
        order.setThirdSyncMsg(message);
        if (finalFailure) {
            order.setStatus(OrderStatusEnum.SYNC_FAILED.getCode());
            order.setThirdSyncStatus(OrderSyncStatusEnum.FAILED.getCode());
            order.setRemark("入职调派订单：第三方同步失败，等待人工处理或补偿任务");
        } else {
            order.setStatus(OrderStatusEnum.WAIT_EXTERNAL_SYNC.getCode());
            order.setThirdSyncStatus(OrderSyncStatusEnum.SYNCING.getCode());
            order.setRemark("入职调派订单：第三方同步失败，等待自动重试");
        }
        this.updateById(order);
        return order;
    }

    @Override
    public void retryExternalSync(String orderNo) {
        Order order = getOrderOrThrow(orderNo);
        if (!OrderStatusEnum.WAIT_EXTERNAL_SYNC.getCode().equals(order.getStatus()) 
                && !OrderStatusEnum.SYNC_FAILED.getCode().equals(order.getStatus())) {
            throw new UnoException("当前订单状态不允许重试第三方同步: " + order.getStatus(), ResultCodeEnum.FAIL.getCode());
        }
        order.setStatus(OrderStatusEnum.WAIT_EXTERNAL_SYNC.getCode());
        order.setThirdSyncStatus(OrderSyncStatusEnum.NOT_SYNCED.getCode());
        order.setThirdSyncMsg("人工触发第三方同步重试");
        order.setRemark("入职调派订单：已重新投递第三方同步消息");
        this.updateById(order);

        List<ProductItemDTO> products = orderItemService.list(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, order.getOrderNo())
        ).stream().map(item -> new ProductItemDTO(item.getProductId(), item.getCount())).collect(Collectors.toList());

        ExternalSyncMsgDTO msg = new ExternalSyncMsgDTO(order.getOrderNo(), order.getEmployeeId(), products, order.getOrderType());
        rocketMQTemplate.convertAndSend("uno-external-sync-topic", msg);
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
        Order order = getOrderOrThrow(orderNo);
        order.setStatus(OrderStatusEnum.SETTLED.getCode());
        order.setRemark("入职调派订单：账单已支付，订单已结算");
        this.updateById(order);
        return order;
    }

    @Override
    public Order advanceStatus(String orderNo) {
        log.info("【人力资源系统-订单中心】正在处理订单流转: {}", orderNo);
        
        Order order = getOrderOrThrow(orderNo);

        String currentStatus = order.getStatus();
        
        // 简易状态机流转设计：CREATED -> PROCESSING -> WAIT_EXTERNAL_SYNC -> PENDING_PAYMENT -> SETTLED -> CLOSED
        // 真实面试时，这里可以说使用的是状态模式(State Pattern)或者是 Spring Statemachine
        if (OrderStatusEnum.CREATED.getCode().equals(currentStatus)) {
            order.setStatus(OrderStatusEnum.PROCESSING.getCode());
        } else if (OrderStatusEnum.PROCESSING.getCode().equals(currentStatus)) {
            order.setStatus(OrderStatusEnum.WAIT_EXTERNAL_SYNC.getCode());
        } else if (OrderStatusEnum.WAIT_EXTERNAL_SYNC.getCode().equals(currentStatus)) {
            order.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        } else if (OrderStatusEnum.PENDING_PAYMENT.getCode().equals(currentStatus)) {
            order.setStatus(OrderStatusEnum.SETTLED.getCode());
        } else if (OrderStatusEnum.SETTLED.getCode().equals(currentStatus)) {
            order.setStatus(OrderStatusEnum.CLOSED.getCode());
        } else {
            throw new UnoException("订单已结束或状态异常，无法推进: " + currentStatus, ResultCodeEnum.FAIL.getCode());
        }

        this.updateById(order);
        log.info("🎯 订单 {} 状态流转成功: {} ➡️ {}", orderNo, currentStatus, order.getStatus());
        
        return order;
    }

    private Order getOrderOrThrow(String orderNo) {
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new UnoException("订单不存在", ResultCodeEnum.FAIL.getCode());
        }
        return order;
    }

    /**
     * 使用 RocketMQ 事务消息改写后的入口
     */
    public void onboardViaTxMsg(OnboardRequestDTO onboardRequestDTO) {
        // 1. 提前生成业务单号（非常重要，因为它是事务的唯一标识，也是反查的依据）
        String orderNo = bizNoGenerator.generate(BizNoPrefixEnum.ONBOARD);
        onboardRequestDTO.setOrderNo(orderNo);

        // 2. 构建 Spring Message 对象
        Message<OnboardRequestDTO> message = MessageBuilder
                .withPayload(onboardRequestDTO)
                .setHeader("KEYS", orderNo) // 把单号塞进 Header，方便反查时提取
                .build();

        // 🚨 3. 发送事务消息（核心巨无霸方法）
        // 参数 1: "uno-external-sync-topic" -> 最终投递的 Topic
        // 参数 2: message -> 消息体
        // 参数 3: onboardRequestDTO -> 传递给本地事务执行器（Listener）的参数对象
        log.info("[事务消息] 🌟 准备向 RocketMQ 发送 Half 消息. OrderNo={}", orderNo);
        
        rocketMQTemplate.sendMessageInTransaction(
                "uno-external-sync-topic", 
                message, 
                onboardRequestDTO
        );
        
        log.info("[事务消息] 🌟 Half 消息发送完成，剩下的交给 Listener 了！OrderNo={}", orderNo);
    }
}
