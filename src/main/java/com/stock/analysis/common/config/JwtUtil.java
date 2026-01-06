package com.stock.analysis.common.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    // 密钥
    private final SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // JWT过期时间（毫秒）
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    /**
     * 从令牌中获取用户名
     * @param token 令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中获取过期时间
     * @param token 令牌
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从令牌中获取声明
     * @param token 令牌
     * @param claimsResolver 声明解析器
     * @param <T> 声明类型
     * @return 声明值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从令牌中获取所有声明
     * @param token 令牌
     * @return 所有声明
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 检查令牌是否过期
     * @param token 令牌
     * @return 是否过期
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 生成令牌
     * @param username 用户名
     * @return 令牌
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    /**
     * 创建令牌
     * @param claims 声明
     * @param subject 主题
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 验证令牌
     * @param token 令牌
     * @param userDetails 用户详情
     * @return 是否有效
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}