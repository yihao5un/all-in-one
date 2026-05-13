package com.uno.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uno.auth.entity.SysUser;
import com.uno.auth.mapper.SysUserMapper;
import com.uno.auth.service.SysUserService;
import com.uno.common.exception.UnoException;
import com.uno.common.result.ResultCodeEnum;
import com.uno.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public String login(String username, String password) {
        log.info("【人力资源系统】正在进行登录校验: {}", username);
        
        // 1. 根据用户名 from 数据库查询
        SysUser sysUser = this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (sysUser == null) {
            throw new UnoException(ResultCodeEnum.USER_NOT_FOUND);
        }
        
        // 2. 校验密码
        if (!sysUser.getPassword().equals(password)) {
            throw new UnoException(ResultCodeEnum.PASSWORD_ERROR);
        }
        
        // 3. 账号状态校验
        if (sysUser.getStatus() != 1) {
            throw new UnoException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        
        // 4. 签发并发放全系统通用的 JWT Token
        return JwtUtils.generateToken(sysUser.getId().toString(), sysUser.getUsername());
    }

    @Override
    public void logout() {
        // 在 JWT 架构中，登出主要是前端删除 Token
        // 后端如果需要更安全，可以在这里将当前 Token 加入 Redis 黑名单
        log.info("【人力资源系统】用户登出成功");
    }
}
