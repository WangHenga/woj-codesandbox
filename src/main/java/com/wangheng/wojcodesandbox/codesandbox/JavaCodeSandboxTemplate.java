package com.wangheng.wojcodesandbox.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeRequest;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeResponse;
import com.wangheng.wojcodesandbox.codesandbox.model.JudgeInfo;
import com.wangheng.wojcodesandbox.enums.QuestionSubmitStatusEnum;
import com.wangheng.wojcodesandbox.exception.UserException;
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
public abstract class JavaCodeSandboxTemplate implements CodeSandbox{
    private final static String GLOBAL_DIR_NAME="tmpFile";
    private final static String GLOBAL_FILE_NAME="Main.java";
    private final static String SECURITY_MANAGER_PATH="E:\\WOJ\\woj-codesandbox\\src\\main\\resources\\security";
    private final static String SECURITY_MANAGER_CLASS_NAME="MySecurityManager";

    private static List<String> blackList= Arrays.asList("File","exec");

    private static WordTree wordTree;
    static {
        wordTree=new WordTree();
        wordTree.addWords(blackList);
    }

    /**
     * 将代码以UTF-8的形式写入文件
     */
    public File writeToFile(String code) throws UserException {
//        List<FoundWord> foundWords = wordTree.matchAllWords(code);
//        if(foundWords.size()!=0){
//            throw new UserException("代码包含敏感操作");
//        }

        String userCodeParentPath=System.getProperty("user.dir")+ File.separator+GLOBAL_DIR_NAME+File.separator+ UUID.randomUUID();

        String userCodePath=userCodeParentPath+File.separator+GLOBAL_FILE_NAME;
        File userCodeFile= FileUtil.touch(userCodePath);
        FileUtil.writeString(code,userCodeFile,"UTF-8");
        return userCodeFile;
    }

    /**
     * 编译java文件
     */
    public void compileFile(File userCodeFile) throws IOException, InterruptedException, UserException {
        ExecuteMessage compileMessage = ProcessUtils.runProcessAndGetMessage(String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath()), "编译");
        if(compileMessage.getExitValue()==null||!compileMessage.getExitValue().equals(0)){
            throw new UserException(compileMessage.getErrorMessage());
        }
    }

    public List<ExecuteMessage> runFile(File userCodeFile,List<String> inputList) throws IOException, InterruptedException {
        /**
         * 运行java文件
         */
        List<ExecuteMessage> runMessageList=new ArrayList<>();
        for (String input : inputList) {
            ExecuteMessage runMessage=ProcessUtils.runInteractProcessAndGetMessage(
                    String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main", userCodeFile.getParent(), SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME),
                    "运行",input
            );
//            ExecuteMessage runMessage = ProcessUtils.runProcessAndGetMessage(
//                    String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",
//                            userCodeFile.getParent(),input), "运行");
            runMessageList.add(runMessage);
        }
        return runMessageList;
    }

    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList){
        /**
         * 整理输出结果
         */
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList=new ArrayList<>();
        long maxTime=0;
        long maxMemory=0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            maxTime=Math.max(maxTime,executeMessage.getTime());
            if(executeMessage.getMemory()!=null) maxMemory=Math.max(maxMemory,executeMessage.getMemory());
            if(executeMessage.getExitValue()!=null&&executeMessage.getExitValue().equals(0)){
                outputList.add(executeMessage.getOutput());
            }else{
                executeCodeResponse.setMessage(executeMessage.getErrorMessage());
                executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
                break;
            }
        }
        if(outputList.size()==executeMessageList.size()){
            executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        if(maxMemory!=0) judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    public boolean deleteFile(File file){
        String userCodeParentPath=file.getParentFile().getAbsolutePath();
        if(FileUtil.exist(userCodeParentPath)){
            FileUtil.del(userCodeParentPath);
            log.info("删除{}成功",userCodeParentPath);
            return true;
        }
        return false;
    }

    public ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        if(e instanceof UserException) executeCodeResponse.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        else executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SYS_WRONG.getValue());
        executeCodeResponse.setMessage(e.getMessage());
        return executeCodeResponse;
    }
    @Override
    public ExecuteCodeResponse doExecute(ExecuteCodeRequest executeCodeRequest) {
        List<ExecuteMessage> executeMessages;
        File file=null;
        try {
            file = writeToFile(executeCodeRequest.getCode());
            compileFile(file);
            executeMessages = runFile(file, executeCodeRequest.getInputList());
        } catch (UserException | IOException | InterruptedException e) {
            return getErrorResponse(e);
        }finally {
            if(file!=null&&!deleteFile(file)){
                log.error("删除{}失败",file);
            }
        }
        return getOutputResponse(executeMessages);
    }
}
