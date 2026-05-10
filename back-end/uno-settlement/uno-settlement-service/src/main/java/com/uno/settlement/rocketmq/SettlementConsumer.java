package com.uno.settlement.rocketmq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uno.common.dto.SettlementMsgDTO;
import com.uno.settlement.entity.Bill;
import com.uno.settlement.service.BillService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 结算中心消息消费者
 * 接收来自订单中心的入职消息，并生成对应的账单
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "uno-settlement-topic", consumerGroup = "uno-settlement-group")
public class SettlementConsumer implements RocketMQListener<SettlementMsgDTO> {

    @Autowired
    private BillService billService;

    @Override
    public void onMessage(SettlementMsgDTO message) {
        log.info("📩 [结算中心] 收到入职结算消息: {}", message);

        // 核心加固：过滤掉 orderNo 为空的“坏消息”，防止旧消息无限重试阻塞链路
        if (message.getOrderNo() == null || message.getOrderNo().isEmpty()) {
            log.warn("⚠️ [结算中心] 收到无效消息 (orderNo 为空)，跳过处理");
            return;
        }

        // 1. 幂等性检查：防止消息重复消费
        // 实际面试中可以说：通过数据库唯一索引 (order_no) 或 Redis 分布式锁实现
        Bill existingBill = billService.getOne(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getOrderNo, message.getOrderNo()));
        
        if (existingBill != null) {
            log.warn("⚠️ [结算中心] 账单已存在，忽略重复消息: OrderNo={}", message.getOrderNo());
            return;
        }

        // 2. 生成账单
        Bill bill = new Bill();
        bill.setBillNo("BILL" + System.currentTimeMillis()); // 简易账单号
        bill.setOrderNo(message.getOrderNo());
        bill.setEmployeeId(message.getEmployeeId());
        bill.setAmount(message.getAmount());
        bill.setBillType("ONBOARD_FEE"); // 入职费用
        bill.setStatus("PENDING");      // 待支付
        bill.setRemark("来自入职事务消息的自动预提");
        
        billService.save(bill);
        
        log.info("✅ [结算中心] 账单生成成功！BillID: {}, OrderNo: {}", bill.getId(), bill.getOrderNo());
    }
}
