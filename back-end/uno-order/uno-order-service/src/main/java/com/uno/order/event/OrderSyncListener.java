package com.uno.order.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uno.common.result.Result;
import com.uno.order.entity.Order;
import com.uno.order.entity.OrderItem;
import com.uno.order.entity.OrderOutbox;
import com.uno.order.es.EsOrderDoc;
import com.uno.order.es.EsOrderRepository;
import com.uno.order.feign.AuthFeignClient;
import com.uno.order.feign.AuthUserDTO;
import com.uno.order.service.OrderItemService;
import com.uno.order.service.OrderOutboxService;
import com.uno.order.service.OrderService;
import com.uno.product.api.ProductFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSyncListener {

    private final EsOrderRepository esOrderRepository;
    private final OrderItemService orderItemService;
    private final OrderOutboxService orderOutboxService;
    private final AuthFeignClient authFeignClient;
    private final ProductFeignClient productFeignClient;
    
    // 使用 @Lazy 解决可能循环依赖问题（OrderService 注入了 Listener，Listener 又注入了 OrderService）
    @Lazy
    private final OrderService orderService;

    @Async // 异步执行，不占用 HTTP 业务线程
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderSync(OrderSyncEvent event) {
        String orderNo = event.getOrderNo();
        log.info("[ES数据同步] 🌟 接收到订单同步事件. OrderNo={}", orderNo);

        try {
            // 1. 查询订单主表
            Order order = orderService.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
            if (order == null) {
                log.warn("[ES数据同步] 查无此订单，跳过同步. OrderNo={}", orderNo);
                return;
            }

            // 2. 批量查订单明细
            List<OrderItem> items = orderItemService.list(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderNo, orderNo));
            Set<Long> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());

            // 3. 远程调用产品中心，获取产品名称并组装
            String productNames = "无产品";
            try {
                Result<List<Map<String, Object>>> productListResult = productFeignClient.list();
                if (productListResult != null && productListResult.getCode() == 200 && productListResult.getData() != null) {
                    productNames = productListResult.getData().stream()
                            .filter(map -> {
                                Object idObj = map.get("id");
                                if (idObj == null) return false;
                                return productIds.contains(Long.valueOf(idObj.toString()));
                            })
                            .map(map -> {
                                Object nameObj = map.get("productName");
                                return nameObj != null ? nameObj.toString() : "未知产品";
                            })
                            .collect(Collectors.joining(", "));
                }
            } catch (Exception e) {
                log.warn("[ES数据同步] 远程获取产品名称失败，将降级处理. Error={}", e.getMessage());
            }

            // 4. 远程调用认证中心，获取员工姓名
            String employeeName = "员工ID: " + order.getEmployeeId();
            try {
                Result<List<AuthUserDTO>> userListResult = authFeignClient.listUsers(null);
                if (userListResult != null && userListResult.getCode() == 200 && userListResult.getData() != null) {
                    employeeName = userListResult.getData().stream()
                            .filter(u -> u.getId().equals(order.getEmployeeId()))
                            .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername())
                            .findFirst()
                            .orElse(employeeName);
                }
            } catch (Exception e) {
                log.warn("[ES数据同步] 远程获取员工姓名失败，将降级处理. Error={}", e.getMessage());
            }

            // 5. 查询 Outbox 消息状态
            boolean billSent = orderOutboxService.count(new LambdaQueryWrapper<OrderOutbox>()
                    .eq(OrderOutbox::getBizNo, orderNo)
                    .eq(OrderOutbox::getEventType, "SETTLEMENT_CREATED")
                    .eq(OrderOutbox::getStatus, "SENT")) > 0;

            // 6. 构造扁平化宽表文档并保存至 ES
            EsOrderDoc doc = EsOrderDoc.builder()
                    .id(order.getId())
                    .orderNo(order.getOrderNo())
                    .employeeId(order.getEmployeeId())
                    .employeeName(employeeName)
                    .orderType(order.getOrderType())
                    .status(order.getStatus())
                    .thirdSyncStatus(order.getThirdSyncStatus())
                    .thirdSyncMsg(order.getThirdSyncMsg())
                    .productNames(productNames)
                    .billSyncSent(billSent)
                    .remark(order.getRemark())
                    .createTime(order.getCreateTime() != null ? order.getCreateTime().toString() : "")
                    .build();

            esOrderRepository.save(doc);
            log.info("[ES数据同步] 🚀 成功将订单同步至 Elasticsearch! OrderNo={}, EmployeeName={}, Products={}",
                    orderNo, employeeName, productNames);

        } catch (Exception e) {
            log.error("[ES数据同步] ❌ 订单同步发生异常，跳过此订单. OrderNo={}, Error={}", orderNo, e.getMessage(), e);
        }
    }
}
