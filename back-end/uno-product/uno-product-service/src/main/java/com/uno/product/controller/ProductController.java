package com.uno.product.controller;

import com.uno.common.result.Result;
import com.uno.common.dto.ProductItemDTO;
import com.uno.product.entity.Product;
import com.uno.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

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
    public Result<Object> deduct(@RequestBody List<ProductItemDTO> productItemDTOS,
                                 @RequestParam("bizNo") String bizNo) {
        for (ProductItemDTO item : productItemDTOS) {
            // 拼接 productId 保证幂等防重对每个产品独立生效
            productService.deductQuota(item.getProductId(), item.getCount(), bizNo + ":" + item.getProductId());
        }
        return Result.success(null);
    }
}
