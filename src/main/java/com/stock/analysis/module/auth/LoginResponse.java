package com.stock.analysis.module.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT令牌
     */
    private String token;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色列表
     */
    private List<String> roles;
}