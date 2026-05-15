package com.uno.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uno.common.dto.ExternalSyncMsgDTO;
import com.uno.common.dto.SettlementMsgDTO;
import com.uno.common.exception.UnoException;
import com.uno.order.entity.OrderOutbox;
import com.uno.order.mapper.OrderOutboxMapper;
import com.uno.order.service.OrderOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderOutboxServiceImpl extends ServiceImpl<OrderOutboxMapper, OrderOutbox> implements OrderOutboxService {

    private static final String EXTERNAL_SYNC_TOPIC = "uno-external-sync-topic";
    private static final String SETTLEMENT_TOPIC = "uno-settlement-topic";
    private static final String EVENT_TYPE_EXTERNAL_SYNC = "EXTERNAL_SYNC_REQUESTED";
    private static final String EVENT_TYPE_SETTLEMENT_CREATED = "SETTLEMENT_CREATED";

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public OrderOutboxServiceImpl(RocketMQTemplate rocketMQTemplate, ObjectMapper objectMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveExternalSyncEvent(ExternalSyncMsgDTO message) {
        saveEvent(message.getOrderNo(), EVENT_TYPE_EXTERNAL_SYNC, EXTERNAL_SYNC_TOPIC, message.getOrderNo(), message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSettlementEvent(SettlementMsgDTO message) {
        saveEvent(message.getOrderNo(), EVENT_TYPE_SETTLEMENT_CREATED, SETTLEMENT_TOPIC, message.getOrderNo(), message);
    }

    private void saveEvent(String bizNo, String eventType, String topic, String messageKey, Object message) {
        OrderOutbox outbox = new OrderOutbox();
        outbox.setBizNo(bizNo);
        outbox.setEventType(eventType);
        outbox.setTopic(topic);
        outbox.setMessageKey(messageKey);
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        try {
            outbox.setPayload(objectMapper.writeValueAsString(message));
            this.save(outbox);
        } catch (DuplicateKeyException e) {
            log.info("[Outbox] 事件已存在，跳过重复写入. BizNo={}, EventType={}", bizNo, eventType);
        } catch (JsonProcessingException e) {
            throw new UnoException("Outbox 消息序列化失败: " + e.getMessage());
        }
    }

    @Override
    public void publishPendingMessages(int batchSize) {
        LocalDateTime now = LocalDateTime.now();
        List<OrderOutbox> messages = this.list(new LambdaQueryWrapper<OrderOutbox>()
                .and(wrapper -> wrapper
                        .in(OrderOutbox::getStatus, "PENDING", "FAILED")
                        .le(OrderOutbox::getNextRetryTime, now)
                        .or()
                        .eq(OrderOutbox::getStatus, "PROCESSING")
                        .le(OrderOutbox::getUpdateTime, now.minusMinutes(5)))
                .orderByAsc(OrderOutbox::getId)
                .last("limit " + batchSize));

        for (OrderOutbox outbox : messages) {
            publishOne(outbox);
        }
    }

    private void publishOne(OrderOutbox outbox) {
        boolean claimed = this.update(new LambdaUpdateWrapper<OrderOutbox>()
                .eq(OrderOutbox::getId, outbox.getId())
                .in(OrderOutbox::getStatus, "PENDING", "FAILED", "PROCESSING")
                .set(OrderOutbox::getStatus, "PROCESSING"));
        if (!claimed) {
            return;
        }

        try {
            Object payload = deserializePayload(outbox);
            Message<Object> message = MessageBuilder.withPayload(payload)
                    .setHeader("KEYS", outbox.getMessageKey())
                    .build();
            rocketMQTemplate.send(outbox.getTopic(), message);

            outbox.setStatus("SENT");
            outbox.setSentTime(LocalDateTime.now());
            outbox.setLastError(null);
            this.updateById(outbox);
            log.info("[Outbox] 消息投递成功. BizNo={}, EventType={}, OutboxId={}",
                    outbox.getBizNo(), outbox.getEventType(), outbox.getId());
        } catch (Exception e) {
            int retryCount = outbox.getRetryCount() == null ? 0 : outbox.getRetryCount();
            outbox.setRetryCount(retryCount + 1);
            outbox.setStatus("FAILED");
            outbox.setLastError(e.getMessage());
            outbox.setNextRetryTime(LocalDateTime.now().plusSeconds(nextDelaySeconds(retryCount + 1)));
            this.updateById(outbox);
            log.warn("[Outbox] 消息投递失败，等待补偿重试. BizNo={}, EventType={}, RetryCount={}, Error={}",
                    outbox.getBizNo(), outbox.getEventType(), retryCount + 1, e.getMessage());
        }
    }

    private Object deserializePayload(OrderOutbox outbox) throws JsonProcessingException {
        if (EVENT_TYPE_EXTERNAL_SYNC.equals(outbox.getEventType())) {
            return objectMapper.readValue(outbox.getPayload(), ExternalSyncMsgDTO.class);
        }
        if (EVENT_TYPE_SETTLEMENT_CREATED.equals(outbox.getEventType())) {
            return objectMapper.readValue(outbox.getPayload(), SettlementMsgDTO.class);
        }
        throw new UnoException("不支持的 Outbox 事件类型: " + outbox.getEventType());
    }

    private long nextDelaySeconds(int retryCount) {
        return Math.min(300, (long) Math.pow(2, Math.min(retryCount, 6)) * 5);
    }
}
