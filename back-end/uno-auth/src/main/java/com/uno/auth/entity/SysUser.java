package com.uno.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@Schema(description = "系统用户/员工实体")
public class SysUser {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "密码")
    private String password;
    
    @Schema(description = "真实姓名")
    private String realName;
    
    @Schema(description = "状态 (1:正常, 0:禁用)")
    private Integer status;

    @Schema(description = "角色 (ADMIN:管理员, EMPLOYEE:普通员工)")
    private String role;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
