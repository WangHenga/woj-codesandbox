package com.wangheng.wojcodesandbox.codesandbox.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.wangheng.wojcodesandbox.codesandbox.CodeSandbox;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeRequest;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeResponse;
import com.wangheng.wojcodesandbox.codesandbox.model.JudgeInfo;
import com.wangheng.wojcodesandbox.enums.QuestionSubmitStatusEnum;
import com.wangheng.wojcodesandbox.util.ExecuteMessage;
import com.wangheng.wojcodesandbox.util.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
public class JavaNativeCodeSandboxOld implements CodeSandbox {
    private static String GLOBAL_DIR_NAME="tmpFile";
    private static String GLOBAL_FILE_NAME="Main.java";

    private static List<String> blackList= Arrays.asList("File","exec");

    private static WordTree wordTree;
    static {
        wordTree=new WordTree();
        wordTree.addWords(blackList);
    }
    @Override
    public ExecuteCodeResponse doExecute(ExecuteCodeRequest executeCodeRequest) {
//        System.setSecurityManager(new MySecurityManager());
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> inputList = executeCodeRequest.getInputList();
        /**
         * 将代码以UTF-8的形式写入文件
         */
        String code = executeCodeRequest.getCode();
        List<FoundWord> foundWords = wordTree.matchAllWords(code);
        if(foundWords.size()!=0){
            executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            executeCodeResponse.setMessage("代码包含敏感操作");
            return executeCodeResponse;
        }

        String userCodeParentPath=System.getProperty("user.dir")+ File.separator+GLOBAL_DIR_NAME+File.separator+UUID.randomUUID();

        String userCodePath=userCodeParentPath+File.separator+GLOBAL_FILE_NAME;
        File userCodeFile=FileUtil.touch(userCodePath);
        FileUtil.writeString(code,userCodeFile,"UTF-8");

        /**
         * 编译java文件
         */
        try {
            ExecuteMessage compileMessage = ProcessUtils.runProcessAndGetMessage(String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath()), "编译");
            if(compileMessage.getExitValue()==null||!compileMessage.getExitValue().equals(0)){
                executeCodeResponse.setMessage(compileMessage.getErrorMessage());
                executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            }
        } catch (IOException | InterruptedException e) {
            return getErrorResponse(e);
        }

        /**
         * 运行java文件
         */
        List<ExecuteMessage> runMessageList=new ArrayList<>();
        try{
            for (String input : inputList) {
//                ExecuteMessage runMessage = ProcessUtils.runInteractProcessAndGetMessage(
//                        String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main",
//                                userCodeFile.getParent(),"/home/woj-codesandbox/src/main/resources/security","MySecurityManager"), "运行", input);
                ExecuteMessage runMessage = ProcessUtils.runInteractProcessAndGetMessage(
                        String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main",
                                userCodeFile.getParent()), "运行", input);
                runMessageList.add(runMessage);
            }
        }catch (IOException | InterruptedException e){
            return getErrorResponse(e);
        }


        /**
         * 整理输出结果
         */
        List<String> outputList=new ArrayList<>();
        long maxTime=0;
        for (ExecuteMessage executeMessage : runMessageList) {
            maxTime=Math.max(maxTime,executeMessage.getTime());
            if(executeMessage.getExitValue()!=null&&executeMessage.getExitValue().equals(0)){
                outputList.add(executeMessage.getOutput());
            }else{
                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
                executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                break;
            }
        }
        if(outputList.size()==runMessageList.size()){
            executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        if(FileUtil.exist(userCodeParentPath)){
            FileUtil.del(userCodeParentPath);
            log.info("删除{}成功",userCodeParentPath);
        }

        return executeCodeResponse;
    }

    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SYS_WRONG.getValue());
        executeCodeResponse.setMessage(e.getMessage());
        return executeCodeResponse;
    }
}
