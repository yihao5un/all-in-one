package com.uno.auth.controller;

import com.uno.common.result.Result;
import com.uno.common.utils.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * 登录发放 Token 测试接口
     */
    @PostMapping("/login")
    public Result<Object> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        // TODO: 数据库真实校验逻辑
        // 此处硬编码用于打通网关测试
        if ("admin".equals(username) && "123456".equals(password)) {
            // 生成 Token
            String token = JwtUtils.generateToken("1001", username);
            
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            return Result.success(data);
        }
        return Result.fail().message("用户名或密码错误");
    }
}
