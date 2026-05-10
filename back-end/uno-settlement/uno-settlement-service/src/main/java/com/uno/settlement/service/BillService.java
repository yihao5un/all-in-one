package com.uno.settlement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uno.settlement.entity.Bill;

public interface BillService extends IService<Bill> {
    /**
     * 生成结算账单
     * @param orderNo 订单号
     * @param employeeId 员工ID
     */
    void createBill(String orderNo, Long employeeId);
}
