package com.uno.settlement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uno.settlement.entity.Bill;
import com.uno.common.dto.ProductItemDTO;
import java.util.List;

public interface BillService extends IService<Bill> {
    /**
     * 生成结算账单
     * @param orderNo 订单号
     * @param employeeId 员工ID
     * @param productIds 产品ID列表
     * @param type 业务类型
     */
    void createBill(String orderNo, Long employeeId, List<ProductItemDTO> products, String type);

    /**
     * 支付账单，并反写订单结算状态。
     */
    Bill payBill(String billNo);

    /**
     * 根据订单号补生成缺失账单。用于 MQ 异常、历史迁移等人工修复场景。
     */
    Bill rebuildBillFromOrder(String orderNo);
}
