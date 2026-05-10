package com.uno.auth.controller;

import com.uno.auth.service.SysUserService;
import com.uno.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 登录发放 Token 接口 (直连 MySQL 版)
     */
    @PostMapping("/login")
    public Result<Object> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        // 调用 Service 层走数据库真实校验
        String token = sysUserService.login(username, password);
        
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        return Result.success(data);
    }
}
