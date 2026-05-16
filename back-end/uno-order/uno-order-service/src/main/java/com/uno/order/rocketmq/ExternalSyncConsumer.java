package com.uno.order.rocketmq;

import com.uno.common.dto.ExternalSyncMsgDTO;
import com.uno.common.dto.SettlementMsgDTO;
import com.uno.order.external.ExternalPartnerClient;
import com.uno.order.external.ExternalSyncRequest;
import com.uno.order.external.ExternalSyncResponse;
import com.uno.order.entity.Order;
import com.uno.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 外部三方同步消费者。
 * 入职核心链路成功后先同步第三方，成功后再投递结算消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "uno-external-sync-topic", consumerGroup = "uno-order-external-sync-group")
public class ExternalSyncConsumer implements RocketMQListener<ExternalSyncMsgDTO> {

    private static final int MAX_RETRY_COUNT = 3;

    private final OrderService orderService;

    private final ExternalPartnerClient externalPartnerClient;

    @Override
    public void onMessage(ExternalSyncMsgDTO message) {
        if (message == null || message.getOrderNo() == null || message.getOrderNo().isBlank()) {
            log.warn("[第三方同步] 收到无效消息，跳过处理");
            return;
        }

        String requestId = null;
        try {
            Order current = orderService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, message.getOrderNo())
            );
            if (current == null) {
                log.warn("[第三方同步] 订单不存在，跳过处理. OrderNo={}", message.getOrderNo());
                return;
            }
            if ("SUCCESS".equals(current.getThirdSyncStatus())) {
                log.info("[第三方同步] 订单已同步成功，跳过重复消息. OrderNo={}", message.getOrderNo());
                return;
            }
            if ("SYNC_FAILED".equals(current.getStatus())) {
                log.warn("[第三方同步] 订单已进入人工处理状态，跳过自动重试. OrderNo={}", message.getOrderNo());
                return;
            }

            requestId = "EXT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            orderService.markExternalSyncing(message.getOrderNo(), requestId);

            ExternalSyncRequest request = new ExternalSyncRequest(
                    requestId,
                    message.getOrderNo(),
                    message.getEmployeeId(),
                    message.getProducts(),
                    message.getType()
            );
            ExternalSyncResponse response = externalPartnerClient.syncOnboardOrder(request);
            if (!response.success()) {
                throw new IllegalStateException(response.getMessage());
            }
            SettlementMsgDTO settlementMsg = new SettlementMsgDTO(
                    message.getOrderNo(),
                    message.getEmployeeId(),
                    message.getProducts(),
                    message.getType()
            );
            orderService.markExternalSyncSuccessAndSaveSettlementEvent(
                    message.getOrderNo(),
                    requestId,
                    response.getCode(),
                    response.getMessage(),
                    settlementMsg
            );
            log.info("[第三方同步] 同步成功，已写入结算 Outbox，等待可靠投递. OrderNo={}", message.getOrderNo());
        } catch (Exception e) {
            Order order = orderService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, message.getOrderNo())
            );
            int retryCount = order == null || order.getThirdRetryCount() == null ? 0 : order.getThirdRetryCount();
            boolean finalFailure = retryCount + 1 >= MAX_RETRY_COUNT;
            orderService.markExternalSyncFailed(
                    message.getOrderNo(),
                    requestId,
                    "EXTERNAL_ERROR",
                    e.getMessage(),
                    finalFailure
            );
            log.warn("[第三方同步] 同步失败. OrderNo={}, RetryCount={}, FinalFailure={}, Error={}",
                    message.getOrderNo(), retryCount + 1, finalFailure, e.getMessage());

            if (!finalFailure) {
                throw new RuntimeException(e);
            }
        }
    }
}
