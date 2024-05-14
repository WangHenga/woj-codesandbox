package com.wangheng.wojcodesandbox.controller;

import cn.hutool.json.JSONUtil;
import com.wangheng.wojcodesandbox.codesandbox.impl.JavaNativeCodeSandbox;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeRequest;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeResponse;
import com.wangheng.wojcodesandbox.service.AuthService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@RestController
@RequestMapping("/codesandbox")
public class MainController {
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;
    @Resource
    private AuthService authService;

    @PostMapping("/executeCode")
    public ExecuteCodeResponse doExecute(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
    HttpServletResponse response) throws NoSuchAlgorithmException, InvalidKeyException {
        String appKey = request.getHeader("appKey");
        // 根据appKey查找对应的appSecret
        String appSecret = authService.getAppSecret(appKey);
        if(appSecret==null){
            response.setStatus(403);
            return null;
        }
        // 判断请求是否超时
        Long timestamp = Long.parseLong(request.getHeader("timestamp"));
        if(System.currentTimeMillis()/1000-timestamp>60){
            response.setStatus(403);
            return null;
        }
        // 判断nonce是否记录过
        String nonce = request.getHeader("nonce");
        if(authService.isNonceExisted(nonce)){
            response.setStatus(403);
            return null;
        }
        // 服务端重新进行签名，和客户端传来的签名进行验证
        String sign = request.getHeader("sign");
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        SecretKeySpec secretKeySpec = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hmacData = mac.doFinal((json + timestamp + nonce).getBytes(StandardCharsets.UTF_8));
        String server_sign= Base64.getEncoder().encodeToString(hmacData);
        if(!server_sign.equals(sign)){
            response.setStatus(403);
            return null;
        }
        return javaNativeCodeSandbox.doExecute(executeCodeRequest);
    }
}
