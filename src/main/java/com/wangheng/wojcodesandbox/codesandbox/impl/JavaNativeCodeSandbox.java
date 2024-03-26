package com.wangheng.wojcodesandbox.codesandbox.impl;

import com.wangheng.wojcodesandbox.codesandbox.JavaCodeSandboxTemplate;
import com.wangheng.wojcodesandbox.util.ExecuteMessage;
import com.wangheng.wojcodesandbox.util.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    private final static String SECURITY_MANAGER_PATH="E:\\WOJ\\woj-codesandbox\\src\\main\\resources\\security";
    private final static String SECURITY_MANAGER_CLASS_NAME="MySecurityManager";
    private ThreadPoolExecutor pool=new ThreadPoolExecutor(
            10,
            15,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20),
            new ThreadPoolExecutor.CallerRunsPolicy());
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) throws InterruptedException, ExecutionException {
        int N=inputList.size();
        List<Future<ExecuteMessage>> futures=new ArrayList<>(N);
        for(int i=0;i<inputList.size();i++){
            int finalI = i;
            Future<ExecuteMessage> future=pool.submit(()->{
                ExecuteMessage runMessage = ProcessUtils.runInteractProcessAndGetMessage(
                        String.format(
                                "java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main",
                                userCodeFile.getParent(),
                                SECURITY_MANAGER_PATH,
                                SECURITY_MANAGER_CLASS_NAME),
                        "运行",inputList.get(finalI)
                );
                return runMessage;
            });
            futures.add(i,future);
        }
        List<ExecuteMessage> runMessageList = new ArrayList<>(N);
        for (Future<ExecuteMessage> future : futures) {
            runMessageList.add(future.get());
        }
        return runMessageList;
    }
}
