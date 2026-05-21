package com.uno.order.feign;

import com.uno.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "uno-auth", contextId = "authFeignClient")
public interface AuthFeignClient {

    @GetMapping("/auth/users")
    Result<List<AuthUserDTO>> listUsers(@RequestParam(value = "keyword", required = false) String keyword);
}
