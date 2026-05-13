package com.uno.order.rocketmq;

import com.uno.common.dto.SettlementMsgDTO;
import com.uno.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 订单中心事务监听器
 * 用于确保本地订单创建与消息发送的原子性
 */
@Slf4j
@Component
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired
    private OrderService orderService;

    /**
     * 执行本地事务：当半消息发送成功后，RocketMQ 会回调此方法
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            SettlementMsgDTO settlementMsg = (SettlementMsgDTO) arg;
            if (settlementMsg == null || settlementMsg.getOrderNo() == null) {
                log.error("[MQ回调] 结算消息为空或订单号为空，回滚消息");
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            
            orderService.onboard(settlementMsg.getEmployeeId(), settlementMsg.getProductId(), settlementMsg.getOrderNo()); 

            log.info("[MQ回调] 本地事务执行成功，订单号: {}，准备提交消息", settlementMsg.getOrderNo());
            return RocketMQLocalTransactionState.COMMIT;
            
        } catch (Exception e) {
            log.error("[MQ回调] 本地事务执行失败，回滚消息: {}", e.getMessage());
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    /**
     * 回查本地事务状态：如果 COMMIT/ROLLBACK 响应丢失，MQ 会调用此方法
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        SettlementMsgDTO settlementMsg = (SettlementMsgDTO) msg.getPayload();
        String orderNo = settlementMsg.getOrderNo();
        log.info("🔍 [MQ回查] 正在检查本地事务状态: OrderNo={}", orderNo);

        if (orderService.existsByOrderNo(orderNo)) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
