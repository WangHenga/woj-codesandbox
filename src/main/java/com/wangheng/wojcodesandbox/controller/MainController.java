package com.wangheng.wojcodesandbox.controller;

import com.wangheng.wojcodesandbox.codesandbox.impl.JavaNativeCodeSandbox;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeRequest;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/codesandbox")
public class MainController {
    private static final String AUTH_REQUEST_HEADER="auth";
    private static final String AUTH_REQUEST_SECRET="secret";
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;
    @PostMapping("/executeCode")
    public ExecuteCodeResponse doExecute(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
    HttpServletResponse response){
        if(!AUTH_REQUEST_SECRET.equals(request.getHeader(AUTH_REQUEST_HEADER))){
            response.setStatus(403);
            return null;
        }
        return javaNativeCodeSandbox.doExecute(executeCodeRequest);
    }
}
