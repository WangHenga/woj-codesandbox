package com.wangheng.wojcodesandbox.service.impl;

import com.wangheng.wojcodesandbox.Mapper.AuthMapper;
import com.wangheng.wojcodesandbox.service.AuthService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    private AuthMapper authMapper;
    @Resource
    private RedisTemplate redisTemplate;
    private final static String AUTH="auth_pairs_";
    @Override
    public String getAppSecret(String appKey) {
        String appSecret = (String) redisTemplate.opsForValue().get(AUTH+appKey);
        if(appSecret==null){
            appSecret=authMapper.getAppSecretByAppKey(appKey);
            redisTemplate.opsForValue().set(AUTH+appKey,appSecret,24, TimeUnit.HOURS);
        }
        return appSecret;
    }

    @Override
    public Boolean isNonceExisted(String nonce) {
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        // 删除过期nonce
        zSetOperations.removeRangeByScore("nonce_zset",0,System.currentTimeMillis()/1000);
        // 判断nonce是否用过
        Double score = zSetOperations.score("nonce_zset", nonce);
        if(score==null){
            zSetOperations.add("nonce_zset",nonce,System.currentTimeMillis()/1000+60);
            return false;
        }
        return true;
    }

}
