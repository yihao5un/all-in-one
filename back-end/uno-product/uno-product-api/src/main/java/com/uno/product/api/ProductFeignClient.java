package com.uno.product.api;

import com.uno.common.dto.ProductItemDTO;
import com.uno.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "uno-product", contextId = "productFeignClient")
public interface ProductFeignClient {

    /**
     * 远程调用扣减产品名额
     */
    @PostMapping("/product/deduct")
    Result<Object> deduct(@RequestBody List<ProductItemDTO> items,
                          @RequestParam("bizNo") String bizNo);

    /**
     * 远程调用获取所有产品列表
     */
    @org.springframework.web.bind.annotation.GetMapping("/product/list")
    Result<List<java.util.Map<String, Object>>> list();
}
