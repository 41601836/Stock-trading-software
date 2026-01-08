package com.stock.analysis.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类，提供常用的Redis操作方法
 */
@Slf4j
@Component
public class RedisUtil {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    /**
     * 设置字符串缓存
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否成功
     */
    public boolean setString(String key, String value, long expireTime, TimeUnit timeUnit) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, expireTime, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Redis set string failed: key={}, value={}, error={}", key, value, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取字符串缓存
     * @param key 键
     * @return 值
     */
    public String getString(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get string failed: key={}, error={}", key, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 设置对象缓存
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否成功
     */
    public <T> boolean setObject(String key, T value, long expireTime, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, expireTime, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Redis set object failed: key={}, value={}, error={}", key, value, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取对象缓存
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis get object failed: key={}, error={}", key, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     * @param key 键
     * @return 是否成功
     */
    public boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis delete failed: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 延长缓存过期时间
     * @param key 键
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否成功
     */
    public boolean expire(String key, long expireTime, TimeUnit timeUnit) {
        try {
            return redisTemplate.expire(key, expireTime, timeUnit);
        } catch (Exception e) {
            log.error("Redis expire failed: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 检查键是否存在
     * @param key 键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis has key failed: key={}, error={}", key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 原子递增
     * @param key 键
     * @param delta 递增步长
     * @return 递增后的值
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis increment failed: key={}, delta={}, error={}", key, delta, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 原子递减
     * @param key 键
     * @param delta 递减步长
     * @return 递减后的值
     */
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("Redis decrement failed: key={}, delta={}, error={}", key, delta, e.getMessage(), e);
            return null;
        }
    }
}