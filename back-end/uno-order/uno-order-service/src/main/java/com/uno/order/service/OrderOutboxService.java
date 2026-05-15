package com.uno.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uno.common.dto.ExternalSyncMsgDTO;
import com.uno.common.dto.SettlementMsgDTO;
import com.uno.order.entity.OrderOutbox;

public interface OrderOutboxService extends IService<OrderOutbox> {
    void saveExternalSyncEvent(ExternalSyncMsgDTO message);

    void saveSettlementEvent(SettlementMsgDTO message);

    void publishPendingMessages(int batchSize);
}
