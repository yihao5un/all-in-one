package com.uno.order.schedule;

import com.uno.order.service.OrderOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderOutboxPublishTask {

    private final OrderOutboxService orderOutboxService;

    public OrderOutboxPublishTask(OrderOutboxService orderOutboxService) {
        this.orderOutboxService = orderOutboxService;
    }

    @Scheduled(fixedDelayString = "${uno.outbox.publish-delay-ms:180000}")
//    @Scheduled(fixedDelayString = "${uno.outbox.publish-delay-ms:5000}")
    public void publishPendingMessages() {
        orderOutboxService.publishPendingMessages(50, null);
    }
}
