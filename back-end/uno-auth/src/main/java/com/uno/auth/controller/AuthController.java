package com.uno.auth.controller;

import com.uno.auth.service.SysUserService;
import com.uno.common.result.Result;
import com.uno.auth.entity.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    /**
     * 登录发放 Token 接口 (直连 MySQL 版)
     */
    private final jakarta.servlet.http.HttpServletResponse response;

    @PostMapping("/login")
    public Result<Object> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        String token = sysUserService.login(username, password);
        
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        
        Map<String, Object> data = new HashMap<>();
        data.put("access_token", token);
        data.put("token_type", "Bearer");
        data.put("expires_in", 86400);
        data.put("role", user != null ? user.getRole() : "EMPLOYEE");
        data.put("username", username);
        data.put("real_name", user != null ? user.getRealName() : username);
        
        // ⚡️ 真正的大厂规范：Token 放在 Response Header 中返回
        response.setHeader("Authorization", "Bearer " + token);
        // 为了前端能拿到这个头，必须暴露它
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        
        return Result.success(data);
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public Result<Object> logout() {
        sysUserService.logout();
        return Result.success();
    }

    /**
     * 员工列表：当前阶段复用 sys_user 作为员工主数据
     */
    @GetMapping("/users")
    public Result<Object> listUsers(@RequestParam(value = "keyword", required = false) String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword);
        }
        return Result.success(sysUserService.list(wrapper));
    }

    /**
     * 启用/停用员工账号
     */
    @PostMapping("/users/{id}/status")
    public Result<Object> updateUserStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        sysUserService.updateById(user);
        return Result.success();
    }

    /**
     * 新增员工
     */
    @PostMapping("/users")
    public Result<Object> addUser(@RequestBody SysUser user) {
        if (user.getRole() == null) {
            user.setRole("EMPLOYEE");
        }
        sysUserService.save(user);
        return Result.success();
    }

    /**
     * 删除员工
     */
    @DeleteMapping("/users/{id}")
    public Result<Object> deleteUser(@PathVariable("id") Long id) {
        sysUserService.removeById(id);
        return Result.success();
    }
}
