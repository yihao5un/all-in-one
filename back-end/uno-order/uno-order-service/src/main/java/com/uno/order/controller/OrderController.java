package com.uno.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uno.common.dto.SettlementMsgDTO;
import com.uno.common.result.Result;
import com.uno.order.dto.OnboardOrderResponse;
import com.uno.order.dto.OrderDTO;
import com.uno.order.entity.Order;
import com.uno.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 查询所有订单 (支持分页)
     */
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(value = "page", defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        Page<Order> orderPage = new Page<>(page, limit);
        return Result.success(orderService.page(orderPage, new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreateTime)));
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/update")
    public Result<Object> update(@RequestBody Order order) {
        orderService.updateById(order);
        return Result.success();
    }

    @PostMapping("/{orderNo}/settle")
    public Result<Object> settle(@PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.markSettled(orderNo));
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable("id") Long id) {
        orderService.removeById(id);
        return Result.success();
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderNo}")
    public Result<OrderDTO> getOrder(@PathVariable String orderNo) {
        Order order = orderService.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return Result.<OrderDTO>fail().message("查无此订单");
        }
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setEmployeeId(order.getEmployeeId());
        dto.setProductId(order.getProductId());
        dto.setOrderType(order.getOrderType());
        dto.setStatus(order.getStatus());
        dto.setRemark(order.getRemark());
        return Result.success(dto);
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
     * 核心业务：全链路入职 (事务消息 + Seata 分布式事务演示)
     * 流程：发送 Half 消息 -> 回调监听器执行本地入职(含 Seata 跨服务调用) -> 本地成功后提交消息 -> 结算中心消费。
     * Controller 不直接执行本地事务，避免与 MQ 事务监听器重复创建订单和扣减产品名额。
     */
    @PostMapping("/onboard")
    public Result<OnboardOrderResponse> onboard(@RequestParam("employeeId") Long employeeId, @RequestParam("productId") Long productId) {
        String orderNo = UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();
        log.info("[订单中心] 接收到入职申请. EmployeeId={}, ProductId={}, OrderNo={}", employeeId, productId, orderNo);

        if (orderService.existsByEmployeeId(employeeId)) {
            return Result.fail(new OnboardOrderResponse(orderNo, "DUPLICATE_ONBOARD"))
                    .message("该员工已存在入职订单，不能重复入职");
        }

        SettlementMsgDTO msgDTO = new SettlementMsgDTO();
        msgDTO.setOrderNo(orderNo);
        msgDTO.setEmployeeId(employeeId);
        msgDTO.setProductId(productId); 
        msgDTO.setType("ONBOARD");
        
        Message<SettlementMsgDTO> message = MessageBuilder.withPayload(msgDTO)
                .setHeader(RocketMQHeaders.KEYS, orderNo)
                .build();

        TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction(
                "uno-settlement-topic",
                message,
                msgDTO // 传给监听器的参数
        );

        if (sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
            return Result.fail(new OnboardOrderResponse(orderNo, "ROLLBACK")).message("该员工已存在入职订单，不能重复入职");
        }
        if (sendResult.getLocalTransactionState() == LocalTransactionState.UNKNOW) {
            return Result.success(new OnboardOrderResponse(orderNo, "PROCESSING")).message("入职事务处理中，请稍后查询订单与结算状态");
        }

        return Result.success(new OnboardOrderResponse(orderNo, "SETTLEMENT_PENDING")).message("入职订单已创建，结算账单生成中");
    }
}
