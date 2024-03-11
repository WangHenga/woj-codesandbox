package com.wangheng.wojcodesandbox.codesandbox.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.wangheng.wojcodesandbox.codesandbox.CodeSandbox;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeRequest;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeResponse;
import com.wangheng.wojcodesandbox.codesandbox.model.JudgeInfo;
import com.wangheng.wojcodesandbox.enums.QuestionSubmitStatusEnum;
import com.wangheng.wojcodesandbox.util.ExecuteMessage;
import com.wangheng.wojcodesandbox.util.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class JavaDockerCodeSandboxOld implements CodeSandbox {
    private static String GLOBAL_DIR_NAME="tmpFile";
    private static String GLOBAL_FILE_NAME="Main.java";

    private static List<String> blackList= Arrays.asList("File","exec");

    private static long TIME_OUT=5000L;

    private static WordTree wordTree;
    static {
        wordTree=new WordTree();
        wordTree.addWords(blackList);
    }
    @Override
    public ExecuteCodeResponse doExecute(ExecuteCodeRequest executeCodeRequest) {
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
            /**
             * 删除文件
             */
            if(FileUtil.exist(userCodeParentPath)){
                FileUtil.del(userCodeParentPath);
                log.info("删除{}成功",userCodeParentPath);
            }
            return getErrorResponse(e);
        }

        /**
         * 运行java文件
         */
        List<ExecuteMessage> runMessageList=new ArrayList<>();
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        /**
         * 创建容器
         */
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd("openjdk:8-alpine");
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        /**
         * 启动容器
         */
        dockerClient.startContainerCmd(containerId).exec();

        /**
         * 操作容器
         */
        try {
            for (String inputArgs : inputList) {
                ExecuteMessage executeMessage=new ExecuteMessage();
                StatsCmd statsCmd = dockerClient.statsCmd(containerId);
                final long[] memory = {0L};
                statsCmd.exec(new ResultCallback<Statistics>() {
                    @Override
                    public void onStart(Closeable closeable) {

                    }

                    @Override
                    public void onNext(Statistics statistics) {
                        memory[0] =Math.max(memory[0],statistics.getMemoryStats().getUsage());
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void close() throws IOException {

                    }
                });
                StopWatch stopWatch=new StopWatch();
                String[] cmdArray =ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"},inputArgs.split(" "));
                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                        .withCmd(cmdArray)
                        .withAttachStderr(true)
                        .withAttachStdin(true)
                        .withAttachStdout(true)
                        .exec();
                System.out.println("创建执行命令：" + execCreateCmdResponse);

                final boolean[] isErr = {false};
                StringBuilder sbErr=new StringBuilder();
                StringBuilder sbOut=new StringBuilder();
                ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        StreamType streamType = frame.getStreamType();
                        if (StreamType.STDERR.equals(streamType)) {
                            isErr[0] =true;
                            String err = new String(frame.getPayload());
                            System.out.println("输出错误结果：" + err);
                            sbErr.append(err).append("\n");
                        } else {
                            String out=new String(frame.getPayload());
                            System.out.println("输出结果：" + out);
                            sbOut.append(out).append("\n");
                        }
                        super.onNext(frame);
                    }
                };
                stopWatch.start();
                boolean timeout = dockerClient.execStartCmd(execCreateCmdResponse.getId())
                        .exec(execStartResultCallback)
                        .awaitCompletion(5L, TimeUnit.SECONDS);
                stopWatch.stop();
                if(isErr[0]) {
                    executeMessage.setExitValue(1);
                    executeMessage.setErrorMessage(sbErr.toString());
                }else if(!timeout){
                    executeMessage.setErrorMessage("运行超时");
                    executeMessage.setExitValue(1);
                }else{
                    executeMessage.setExitValue(0);
                    executeMessage.setOutput(sbOut.toString());
                }

                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
                executeMessage.setMemory(memory[0]);
                statsCmd.close();
                runMessageList.add(executeMessage);
            }
        }catch (Exception e) {
            executeCodeResponse=getErrorResponse(e);
        }

        /**
         * 删除容器
         */
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        /**
         * 删除文件
         */
        if(FileUtil.exist(userCodeParentPath)){
            FileUtil.del(userCodeParentPath);
            log.info("删除{}成功",userCodeParentPath);
        }
        if(QuestionSubmitStatusEnum.SYS_WRONG.getValue().equals(executeCodeResponse.getStatus())){
            return executeCodeResponse;
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

        return executeCodeResponse;
    }

    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SYS_WRONG.getValue());
        executeCodeResponse.setMessage(e.getMessage());
        return executeCodeResponse;
    }
}
