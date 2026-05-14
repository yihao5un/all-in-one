package com.uno.product.api;

import com.uno.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "uno-product", contextId = "productFeignClient")
public interface ProductFeignClient {

    /**
     * 远程调用扣减产品名额
     */
    @PostMapping("/product/deduct")
    Result<Object> deduct(@RequestParam("productId") Long productId,
                          @RequestParam("count") Integer count,
                          @RequestParam("bizNo") String bizNo);
}
