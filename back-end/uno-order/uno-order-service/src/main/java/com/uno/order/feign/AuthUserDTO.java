package com.uno.order.feign;

import lombok.Data;

@Data
public class AuthUserDTO {
    private Long id;
    private String username;
    private String realName;
}
