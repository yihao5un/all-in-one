package com.uno.product.controller;

import com.uno.common.result.Result;
import com.uno.product.entity.Product;
import com.uno.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public Result<Object> list() {
        List<Product> list = productService.list();
        return Result.success(list);
    }

    @PostMapping("/deduct")
    public Result<Object> deduct(@RequestParam("productId") Long productId, @RequestParam("count") Integer count) {
        productService.deductQuota(productId, count);
        return Result.success(null);
    }
}
