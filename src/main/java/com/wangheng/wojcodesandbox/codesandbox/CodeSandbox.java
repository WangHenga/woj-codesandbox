package com.wangheng.wojcodesandbox.codesandbox;


import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeRequest;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeResponse;

public interface CodeSandbox {
    ExecuteCodeResponse doExecute(ExecuteCodeRequest executeCodeRequest);
}
