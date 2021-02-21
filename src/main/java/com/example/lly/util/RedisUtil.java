package com.example.lly.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;


@Component
public class RedisUtil {

    private static final Logger logger = LoggerFactory.getLogger(RedisUtil.class);

    public static final String KEY_PREFIX_VALUE = "seckill:value:";
    @Resource
    private RedisTemplate<Serializable, Serializable> redisTemplate;

    public boolean cacheValue(String k, Serializable v, long time) {
        String key = KEY_PREFIX_VALUE + k;
        try {
            ValueOperations<Serializable, Serializable> valueOps = redisTemplate.opsForValue();
            valueOps.set(key, v);
            if (time > 0) redisTemplate.expire(key, time, TimeUnit.SECONDS);
            return true;
        } catch (Throwable t) {
            logger.error("缓存[{}]失败, value[{}]", key, v, t);
        }
        return false;
    }


    public boolean cacheValue(String k, Serializable v, long time, TimeUnit unit) {
        String key = KEY_PREFIX_VALUE + k;
        try {
            ValueOperations<Serializable, Serializable> valueOps = redisTemplate.opsForValue();
            valueOps.set(key, v);
            if (time > 0) redisTemplate.expire(key, time, unit);
            return true;
        } catch (Throwable t) {
            logger.error("缓存[{}]失败, value[{}]", key, v, t);
        }
        return false;
    }


    public boolean cacheValue(String k, Serializable v) {
        return cacheValue(k, v, -1);
    }


    public boolean containsValueKey(String k) {
        String key = KEY_PREFIX_VALUE + k;
        try {
            return redisTemplate.hasKey(key);
        } catch (Throwable t) {
            logger.error("判断缓存存在失败key[" + key + ", error[" + t + "]");
        }
        return false;
    }


    public Serializable getValue(String k) {
        try {
            ValueOperations<Serializable, Serializable> valueOps = redisTemplate.opsForValue();
            return valueOps.get(KEY_PREFIX_VALUE + k);
        } catch (Throwable t) {
            logger.error("获取缓存失败key[" + KEY_PREFIX_VALUE + k + ", error[" + t + "]");
        }
        return null;
    }


    public boolean removeValue(String k) {
        String key = KEY_PREFIX_VALUE + k;
        try {
            redisTemplate.delete(key);
            return true;
        } catch (Throwable t) {
            logger.error("获取缓存失败key[" + key + ", error[" + t + "]");
        }
        return false;
    }


    public long increase(String k, long delta) {
        String key = KEY_PREFIX_VALUE + k;
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }


    public long decrease(String k, long delta) {
        String key = KEY_PREFIX_VALUE + k;
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }
}

