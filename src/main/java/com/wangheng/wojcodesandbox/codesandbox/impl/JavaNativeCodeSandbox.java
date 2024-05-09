package com.wangheng.wojcodesandbox.codesandbox.impl;

import com.wangheng.wojcodesandbox.codesandbox.JavaCodeSandboxTemplate;
import com.wangheng.wojcodesandbox.util.ExecuteMessage;
import com.wangheng.wojcodesandbox.util.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) throws InterruptedException{
        int N=inputList.size();
        List<ExecuteMessage> runMessageList = new ArrayList<>(N);
        CountDownLatch countDownLatch = new CountDownLatch(inputList.size());
        for(int i=0;i<inputList.size();i++){
            int finalI = i;
            pool.submit(()->{
                ExecuteMessage runMessage = null;
                try {
                    runMessage = ProcessUtils.runInteractProcessAndGetMessage(
                            String.format(
                                    "java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main",
                                    userCodeFile.getParent(),
                                    SECURITY_MANAGER_PATH,
                                    SECURITY_MANAGER_CLASS_NAME),
                            "运行",inputList.get(finalI)
                    );
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                runMessageList.add(finalI,runMessage);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        return runMessageList;
    }
}
