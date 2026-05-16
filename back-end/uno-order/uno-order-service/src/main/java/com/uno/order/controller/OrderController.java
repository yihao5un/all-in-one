package com.uno.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uno.common.enums.BizNoPrefixEnum;
import com.uno.common.result.Result;
import com.uno.order.dto.OnboardOrderResponse;
import com.uno.order.dto.OnboardRequestDTO;
import com.uno.order.dto.OrderDTO;
import com.uno.order.entity.Order;
import com.uno.order.enums.OrderStatusEnum;
import com.uno.order.entity.OrderItem;
import com.uno.order.entity.OrderOutbox;
import com.uno.order.service.OrderItemService;
import com.uno.order.service.OrderOutboxService;
import com.uno.order.service.OrderService;
import org.springframework.beans.BeanUtils;
import java.util.stream.Collectors;
import java.util.List;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "订单管理接口", description = "提供订单查询、状态流转及全链路入职申请等功能")
public class OrderController {

    private final OrderService orderService;
    private final OrderOutboxService orderOutboxService;
    private final OrderItemService orderItemService;

    // 已移除 idGenerator，由 Service 内部处理逻辑

    /**
     * 查询所有订单 (支持分页)
     */
    @GetMapping("/list")
    public Result<Object> list(@RequestParam(value = "page", defaultValue = "1") Integer page,
                               @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        Page<Order> orderPage = new Page<>(page, limit);
        orderService.page(orderPage, new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreateTime));
        
        List<OrderDTO> dtos = orderPage.getRecords().stream().map(order -> {
            OrderDTO dto = new OrderDTO();
            BeanUtils.copyProperties(order, dto);
            
            // 查询订单明细表，获取产品 ID 列表
            List<Long> productIds = orderItemService.list(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderNo, order.getOrderNo()))
                    .stream()
                    .map(OrderItem::getProductId)
                    .collect(Collectors.toList());
            
            dto.setProductIds(productIds);
            
            // 检查结算消息是否已投递
            boolean billSent = orderOutboxService.count(new LambdaQueryWrapper<OrderOutbox>()
                    .eq(OrderOutbox::getBizNo, order.getOrderNo())
                    .eq(OrderOutbox::getEventType, "SETTLEMENT_CREATED")
                    .eq(OrderOutbox::getStatus, "SENT")) > 0;
            dto.setBillSyncSent(billSent);
            
            return dto;
        }).collect(Collectors.toList());
        
        Page<OrderDTO> dtoPage = new Page<>(page, limit);
        dtoPage.setRecords(dtos);
        dtoPage.setTotal(orderPage.getTotal());
        
        return Result.success(dtoPage);
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
     * 手动触发 Outbox 消息投递
     */
    @PostMapping("/outbox/publish")
    public Result<Object> publishOutbox(@RequestParam(value = "eventType", required = false) String eventType) {
        orderOutboxService.publishPendingMessages(50, eventType);
        return Result.success();
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
        OrderDTO dto = orderService.getOrderDTO(orderNo);
        if (dto == null) {
            return Result.<OrderDTO>fail().message("查无此订单");
        }
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
     * 人工补偿入口：重新投递第三方同步消息。
     */
    @PostMapping("/{orderNo}/external-sync/retry")
    public Result<Object> retryExternalSync(@PathVariable("orderNo") String orderNo) {
        orderService.retryExternalSync(orderNo);
        return Result.success().message("第三方同步重试消息已投递");
    }

    /**
     * 核心业务：全链路入职。
     * 流程：Seata 保证订单创建和产品扣减强一致，核心事务内写入三方同步 Outbox，
     * 后台任务再可靠投递 MQ 触发三方同步。
     */
    @Operation(summary = "提交全链路入职申请", description = "包含订单创建、产品名额扣减以及三方系统异步同步流程")
    @PostMapping("/onboard")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<OnboardOrderResponse> onboard(@Valid @RequestBody OnboardRequestDTO onboardRequestDTO) {
        log.info("[订单中心] 接收到入职请求. EmployeeId={}, Products={}", 
                onboardRequestDTO.getEmployeeId(), onboardRequestDTO.getProducts());
        
        String orderNo = orderService.onboard(onboardRequestDTO);
        
        log.info("[订单中心] 入职请求处理成功. OrderNo={}", orderNo);
        
        return Result.success(new OnboardOrderResponse(orderNo, OrderStatusEnum.WAIT_EXTERNAL_SYNC.getCode()))
                .message("入职申请已提交: " + OrderStatusEnum.WAIT_EXTERNAL_SYNC.getDesc());
    }
}
