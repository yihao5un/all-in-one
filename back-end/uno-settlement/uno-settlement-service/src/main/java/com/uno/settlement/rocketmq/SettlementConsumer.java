package com.uno.settlement.rocketmq;

import com.uno.common.dto.SettlementMsgDTO;
import com.uno.settlement.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 结算中心消息消费者
 * 接收来自订单中心的入职消息，并生成对应的账单
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "uno-settlement-topic", consumerGroup = "uno-settlement-group")
public class SettlementConsumer implements RocketMQListener<SettlementMsgDTO> {

    private final BillService billService;

    @Override
    public void onMessage(SettlementMsgDTO message) {
        log.info("📩 [结算中心] 收到入职结算消息: {}", message);

        // 核心加固：过滤掉 orderNo 为空的“坏消息”，防止旧消息无限重试阻塞链路
        if (message.getOrderNo() == null || message.getOrderNo().isEmpty()) {
            log.warn("⚠️ [结算中心] 收到无效消息 (orderNo 为空)，跳过处理");
            return;
        }

        // BillService 内部先按 orderNo 做幂等检查，数据库唯一索引 uk_order_no 兜底。
        billService.createBill(message.getOrderNo(), message.getEmployeeId(), message.getProducts(), message.getType());
        
        log.info("✅ [结算中心] 账单处理完成！OrderNo: {}", message.getOrderNo());
    }
}
