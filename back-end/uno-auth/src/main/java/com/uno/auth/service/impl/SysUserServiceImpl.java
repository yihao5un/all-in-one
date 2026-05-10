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
        
        // 1. 根据用户名从数据库查询
        SysUser sysUser = this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (sysUser == null) {
            throw new UnoException("账号不存在", ResultCodeEnum.FAIL.getCode());
        }
        
        // 2. 校验密码（这里为了快速跑通 Demo使用明文，实际人力资源生产系统需结合 BCryptPasswordEncoder）
        if (!sysUser.getPassword().equals(password)) {
            throw new UnoException("密码错误", ResultCodeEnum.FAIL.getCode());
        }
        
        // 3. 账号状态校验
        if (sysUser.getStatus() != 1) {
            throw new UnoException("账号已被禁用，请联系HR系统管理员", ResultCodeEnum.FAIL.getCode());
        }
        
        // 4. 签发并发放全系统通用的 JWT Token
        return JwtUtils.generateToken(sysUser.getId().toString(), sysUser.getUsername());
    }
}
