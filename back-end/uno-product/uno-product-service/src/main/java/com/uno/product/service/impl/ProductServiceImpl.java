package com.uno.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.common.exception.UnoException;
import com.uno.common.idempotent.Idempotent;
import com.uno.common.lock.DistributedLock;
import com.uno.common.result.ResultCodeEnum;
import com.uno.product.entity.Product;
import com.uno.product.mapper.ProductMapper;
import com.uno.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    @Idempotent(key = "#bizNo", expire = 1, unit = TimeUnit.DAYS)
    @DistributedLock(key = "'product:' + #productId", waitTime = 5, leaseTime = 10)
    @Transactional(rollbackFor = Exception.class)
    public void deductQuota(Long productId, Integer count, String bizNo) {
        log.info("【人力资源系统-产品中心】正在扣减名额: ProductID={}, Count={}, BizNo={}", productId, count, bizNo);
        
        Product product = this.getById(productId);
        if (product == null) {
            throw new UnoException("产品不存在", ResultCodeEnum.FAIL.getCode());
        }

        if (product.getTotalQuota() - product.getUsedQuota() < count) {
            throw new UnoException("产品名额不足: " + product.getProductName(), ResultCodeEnum.FAIL.getCode());
        }

        product.setUsedQuota(product.getUsedQuota() + count);
        this.updateById(product);
        
        log.info("🎯 名额扣减成功: {} 当前已使用 {}", product.getProductName(), product.getUsedQuota());
    }
}
