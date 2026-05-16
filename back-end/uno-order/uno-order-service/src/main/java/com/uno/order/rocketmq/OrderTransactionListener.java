package com.uno.order.rocketmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uno.order.dto.OnboardRequestDTO;
import com.uno.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 事务消息监听器
 * 负责执行本地事务和提供事务状态反查
 */
@Slf4j
@Component
@RocketMQTransactionListener
@RequiredArgsConstructor
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * 执行本地事务
     * 当 RocketMQ 成功接收到 Half 消息后，会自动回调这个方法
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        OnboardRequestDTO requestDTO = (OnboardRequestDTO) arg;
        String orderNo = requestDTO.getOrderNo();
        
        log.info("[事务消息] 收到 Half 消息确认，开始执行本地事务. OrderNo={}", orderNo);
        
        try {
            // 调用原有的业务逻辑：创建订单、扣减名额等
            // 注意：这里的方法需要有事务保证（比如加了 @Transactional 或 Seata 的 @GlobalTransactional）
            orderService.onboard(requestDTO);
            
            // 本地事务执行成功，通知 MQ 提交消息（消息将对消费者可见）
            log.info("[事务消息] 本地事务执行成功，通知 MQ 提交(COMMIT). OrderNo={}", orderNo);
            return RocketMQLocalTransactionState.COMMIT;
            
        } catch (Exception e) {
            // 本地事务执行失败，通知 MQ 回滚消息（消息将被删除，消费者永远看不到）
            log.error("[事务消息] 本地事务执行失败，通知 MQ 回滚(ROLLBACK). OrderNo={}, Error={}", orderNo, e.getMessage());
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 事务状态反查
     * 当 MQ Broker 超过时间没有收到 COMMIT/ROLLBACK 确认时，会主动调用这个方法反查本地事务状态
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        try {
            // 从消息中解析出业务单号
            // 在 Spring Message 中，我们可以从 Header 中获取我们塞进去的 "KEYS"
            String orderNo = (String) msg.getHeaders().get("KEYS");
            log.info("[事务消息] 收到 MQ 反查请求. OrderNo={}", orderNo);
            
            // 通过业务单号去数据库查一下，订单到底建成功了没有
            boolean exists = orderService.existsByOrderNo(orderNo);
            
            if (exists) {
                log.info("[事务消息] 反查结果：订单存在，通知 MQ 提交(COMMIT). OrderNo={}", orderNo);
                return RocketMQLocalTransactionState.COMMIT;
            } else {
                // 【最佳实践优化】：如果订单不存在，不要急着回滚！
                // 返回 UNKNOWN（未知），让 MQ 过一会儿再来反查一次，给本地慢事务留出时间！
                log.warn("[事务消息] 反查结果：订单尚未落库，返回 UNKNOWN 等待下次反查. OrderNo={}", orderNo);
                return RocketMQLocalTransactionState.UNKNOWN;
            }
        } catch (Exception e) {
            log.error("[事务消息] 反查逻辑发生异常: {}", e.getMessage());
            // 如果反查抛异常，返回 UNKNOWN，MQ 会隔段时间再次反查
            return RocketMQLocalTransactionState.UNKNOWN;
        }
    }
}
