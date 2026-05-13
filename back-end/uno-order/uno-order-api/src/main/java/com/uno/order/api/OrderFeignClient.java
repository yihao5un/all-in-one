package com.uno.order.api;

import com.uno.common.result.Result;
import com.uno.order.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "uno-order", contextId = "orderFeignClient")
public interface OrderFeignClient {

    @GetMapping("/order/{orderNo}")
    Result<OrderDTO> getOrder(@PathVariable("orderNo") String orderNo);

    @PostMapping("/order/{orderNo}/settle")
    Result<Object> settle(@PathVariable("orderNo") String orderNo);
}
