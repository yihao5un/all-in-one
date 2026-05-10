package com.uno.settlement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.settlement.entity.Bill;
import com.uno.settlement.mapper.BillMapper;
import com.uno.settlement.service.BillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements BillService {

    @Override
    public void createBill(String orderNo, Long employeeId) {
        log.info("【人力资源系统-结算中心】收到结算请求: OrderNo={}, EmployeeID={}", orderNo, employeeId);
        
        Bill bill = new Bill();
        bill.setBillNo("BILL" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        bill.setOrderNo(orderNo);
        bill.setEmployeeId(employeeId);
        // 模拟计算薪资/账单金额（真实场景会更复杂）
        bill.setAmount(new BigDecimal("5000.00")); 
        bill.setStatus(0); // 待支付
        
        this.save(bill);
        log.info("🎯 账单生成成功: BillNo={}, Amount={}", bill.getBillNo(), bill.getAmount());
    }
}
