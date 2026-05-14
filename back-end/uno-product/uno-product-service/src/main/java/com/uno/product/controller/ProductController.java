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

    @PostMapping("/add")
    public Result<Object> add(@RequestBody Product product) {
        productService.save(product);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Object> update(@RequestBody Product product) {
        productService.updateById(product);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable("id") Long id) {
        productService.removeById(id);
        return Result.success();
    }

    @PostMapping("/deduct")
    public Result<Object> deduct(@RequestParam("productId") Long productId,
                                 @RequestParam("count") Integer count,
                                 @RequestParam("bizNo") String bizNo) {
        productService.deductQuota(productId, count, bizNo);
        return Result.success(null);
    }
}
