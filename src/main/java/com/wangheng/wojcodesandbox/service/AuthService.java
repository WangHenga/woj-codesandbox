package com.wangheng.wojcodesandbox.service;

public interface AuthService {
    String getAppSecret(String appKey);
    Boolean isNonceExisted(String nonce);
}
