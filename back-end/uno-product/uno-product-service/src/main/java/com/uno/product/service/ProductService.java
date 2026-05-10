package com.uno.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uno.product.entity.Product;

public interface ProductService extends IService<Product> {
    /**
     * 扣减产品名额
     * @param productId 产品ID
     * @param count 扣减数量
     */
    void deductQuota(Long productId, Integer count);
}
