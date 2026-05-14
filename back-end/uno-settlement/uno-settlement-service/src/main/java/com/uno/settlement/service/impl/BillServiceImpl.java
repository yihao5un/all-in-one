package com.uno.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.common.exception.UnoException;
import com.uno.common.lock.DistributedLock;
import com.uno.common.result.Result;
import com.uno.order.api.OrderFeignClient;
import com.uno.order.dto.OrderDTO;
import com.uno.settlement.entity.Bill;
import com.uno.settlement.mapper.BillMapper;
import com.uno.settlement.service.BillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements BillService {

    private static final BigDecimal ONBOARD_FEE_AMOUNT = new BigDecimal("5000.00");

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBill(String orderNo, Long employeeId, Long productId, String type) {
        log.info("【人力资源系统-结算中心】收到结算请求: OrderNo={}, EmployeeID={}, ProductID={}, Type={}", orderNo, employeeId, productId, type);

        Bill existing = this.getOne(new LambdaQueryWrapper<Bill>().eq(Bill::getOrderNo, orderNo));
        if (existing != null) {
            log.info("账单已存在，跳过重复生成: BillNo={}, OrderNo={}", existing.getBillNo(), orderNo);
            return;
        }
        
        Bill bill = new Bill();
        bill.setBillNo("BILL" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        bill.setOrderNo(orderNo);
        bill.setEmployeeId(employeeId);
        bill.setAmount(calculateAmount(productId, type));
        bill.setBillType(resolveBillType(type));
        bill.setStatus("PENDING"); // 待支付
        bill.setRemark("订单中心事务消息触发生成");
        
        try {
            this.save(bill);
            log.info("🎯 账单生成成功: BillNo={}, Amount={}", bill.getBillNo(), bill.getAmount());
        } catch (DuplicateKeyException e) {
            Bill duplicated = this.getOne(new LambdaQueryWrapper<Bill>().eq(Bill::getOrderNo, orderNo));
            log.info("账单唯一键兜底命中，按消费成功处理: BillNo={}, OrderNo={}",
                    duplicated == null ? null : duplicated.getBillNo(), orderNo);
        }
    }

    @Override
    @DistributedLock(key = "'bill:pay:' + #billNo", waitTime = 3, leaseTime = -1)
    @Transactional(rollbackFor = Exception.class)
    public Bill payBill(String billNo) {
        Bill bill = this.getOne(new LambdaQueryWrapper<Bill>().eq(Bill::getBillNo, billNo));
        if (bill == null) {
            throw new UnoException("账单不存在");
        }
        if ("PAID".equals(bill.getStatus())) {
            log.info("账单已支付，幂等返回: BillNo={}", billNo);
            return bill;
        }
        if (!"PENDING".equals(bill.getStatus())) {
            throw new UnoException("当前账单状态不允许支付: " + bill.getStatus());
        }

        Result<OrderDTO> orderResult = orderFeignClient.getOrder(bill.getOrderNo());
        if (orderResult == null || orderResult.getCode() == null || orderResult.getCode() != 200 || orderResult.getData() == null) {
            throw new UnoException("关联订单不存在，无法支付账单");
        }
        if (!"PENDING_PAYMENT".equals(orderResult.getData().getStatus())) {
            throw new UnoException("关联订单状态不允许支付: " + orderResult.getData().getStatus());
        }

        bill.setStatus("PAID");
        bill.setRemark("账单已支付，已反写订单结算状态");
        this.updateById(bill);

        Result<Object> settleResult = orderFeignClient.settle(bill.getOrderNo());
        if (settleResult == null || settleResult.getCode() == null || settleResult.getCode() != 200) {
            throw new UnoException("订单结算状态反写失败");
        }
        return bill;
    }

    @Override
    public Bill rebuildBillFromOrder(String orderNo) {
        Bill existing = this.getOne(new LambdaQueryWrapper<Bill>().eq(Bill::getOrderNo, orderNo));
        if (existing != null) {
            return existing;
        }

        Result<OrderDTO> orderResult = orderFeignClient.getOrder(orderNo);
        if (orderResult == null || orderResult.getCode() == null || orderResult.getCode() != 200 || orderResult.getData() == null) {
            throw new UnoException("订单不存在，无法补生成账单");
        }

        OrderDTO order = orderResult.getData();
        if (!"ONBOARD".equals(order.getOrderType())) {
            throw new UnoException("当前只支持为入职订单补生成账单");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new UnoException("只有待支付订单可以补生成账单");
        }

        createBill(order.getOrderNo(), order.getEmployeeId(), order.getProductId(), order.getOrderType());
        return this.getOne(new LambdaQueryWrapper<Bill>().eq(Bill::getOrderNo, orderNo));
    }

    private BigDecimal calculateAmount(Long productId, String type) {
        // 当前阶段使用服务内集中计费规则；后续可迁移到 t_bill_rule 按 product_id + bill_type 配置。
        if ("ONBOARD".equals(type)) {
            return ONBOARD_FEE_AMOUNT;
        }
        return BigDecimal.ZERO;
    }

    private String resolveBillType(String type) {
        if ("ONBOARD".equals(type)) {
            return "ONBOARD_FEE";
        }
        return "SERVICE_FEE";
    }
}
