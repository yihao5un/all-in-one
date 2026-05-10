package com.uno.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uno.auth.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    
    /**
     * 核心登录业务
     * @param username 用户名
     * @param password 密码
     * @return JWT Token
     */
    String login(String username, String password);
}
