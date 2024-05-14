package com.wangheng.wojcodesandbox;

import com.wangheng.wojcodesandbox.codesandbox.CodeSandbox;
import com.wangheng.wojcodesandbox.codesandbox.impl.JavaDockerCodeSandbox;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeRequest;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeResponse;
import com.wangheng.wojcodesandbox.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;


@SpringBootTest
class WojCodesandboxApplicationTests {

    @Test
    void contextLoads() {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest(Arrays.asList("1 2", "3 3"), "java",
                "public class Main{\n" +
                "\tpublic static void main(String[] args){\n" +
                "\t\tint a=Integer.parseInt(args[0]);\n" +
                "\t\tint b=Integer.parseInt(args[1]);\n" +
                "\t\tSystem.out.println(a+b);\n" +
                "\t}\n" +
                "}");
        CodeSandbox codeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeResponse response = codeSandbox.doExecute(executeCodeRequest);
        System.out.println(response);
    }
    @Resource
    private AuthService authService;
    @Test
    void testDataSource(){
        String first = authService.getAppSecret("second");
        System.out.println(first);
    }

}
