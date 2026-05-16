package com.uno.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uno.common.dto.SettlementMsgDTO;
import com.uno.order.dto.OnboardRequestDTO;
import com.uno.order.entity.Order;
import com.uno.order.dto.OrderDTO;
import jakarta.validation.Valid;

public interface OrderService extends IService<Order> {
    
    /**
     * 核心业务：推进订单状态 (简易版状态机)
     * @param orderNo 订单号
     * @return 更新后的订单信息
     */
    Order advanceStatus(String orderNo);

    /**
     * 入职全链路业务 (演示 Seata 分布式事务)
     * @param onboardRequestDTO 入职请求 DTO (包含员工、产品及订单号)
     * @return 订单号
     */
    String onboard(@Valid OnboardRequestDTO onboardRequestDTO);

    /**
     * 获取订单详情（包含产品列表）
     */
    OrderDTO getOrderDTO(String orderNo);

    /**
     * 外部三方同步开始。
     */
    Order markExternalSyncing(String orderNo, String requestId);

    /**
     * 外部三方同步成功，订单进入待支付。
     */
    Order markExternalSyncSuccess(String orderNo, String requestId, String responseCode, String message);

    /**
     * 外部三方同步成功后，在同一个本地事务内推进订单状态并写入结算 Outbox。
     */
    Order markExternalSyncSuccessAndSaveSettlementEvent(String orderNo, String requestId, String responseCode, String message, SettlementMsgDTO settlementMsg);

    /**
     * 外部三方同步失败，等待 MQ 重试或人工补偿。
     */
    Order markExternalSyncFailed(String orderNo, String requestId, String responseCode, String message, boolean finalFailure);

    /**
     * 人工或补偿任务重新投递第三方同步消息。
     */
    void retryExternalSync(String orderNo);

    /**
     * 查询订单是否已成功落库。
     */
    boolean existsByOrderNo(String orderNo);

    /**
     * 查询员工是否已有入职订单。
     */
    boolean existsByEmployeeId(Long employeeId);

    /**
     * 支付完成后关闭订单结算链路。
     */
    Order markSettled(String orderNo);
}
