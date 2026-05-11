package com.uno.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uno.common.dto.SettlementMsgDTO;
import com.uno.common.result.Result;
import com.uno.common.idempotent.Idempotent;
import com.uno.order.entity.Order;
import com.uno.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
     * 查询订单详情
     */
    @GetMapping("/{orderNo}")
    public Result<Object> getOrder(@PathVariable("orderNo") String orderNo) {
        Order order = orderService.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return Result.fail().message("查无此订单");
        }
        return Result.success(order);
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
     * 流程：发送 Half 消息 -> 回调监听器执行本地入职(含 Seata 跨服务调用) -> 本地成功后提交消息 -> 结算中心消费
     */
    @PostMapping("/onboard")
    @Idempotent(key = "#employeeId", message = "申请已接收，请勿重复点击")
    public Result<Object> onboard(@RequestParam("employeeId") Long employeeId, @RequestParam("productId") Long productId) {
        log.info("📢 [订单中心] 接收到入职申请，准备发送事务消息. EmployeeId: {}", employeeId);

        // 提前生成订单号 (这是分布式消息一致性的核心：ID 提前生成)
        String orderNo = "ONB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // 构建结算消息实体
        SettlementMsgDTO msgDTO = new SettlementMsgDTO();
        msgDTO.setOrderNo(orderNo); // 此时 orderNo 已有值！
        msgDTO.setEmployeeId(employeeId);
        msgDTO.setProductId(productId); 
        msgDTO.setAmount(new BigDecimal("2000.00")); 
        msgDTO.setType("ONBOARD");
        
        // 发送事务消息
        rocketMQTemplate.sendMessageInTransaction(
                "uno-settlement-topic",
                MessageBuilder.withPayload(msgDTO).build(),
                msgDTO // 传给监听器的参数
        );

        return Result.success("入职事务已启动，请稍后查询订单与结算状态");
    }
}
