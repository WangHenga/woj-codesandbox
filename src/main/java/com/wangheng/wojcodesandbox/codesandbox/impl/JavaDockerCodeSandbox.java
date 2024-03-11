package com.wangheng.wojcodesandbox.codesandbox.impl;

import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.wangheng.wojcodesandbox.codesandbox.JavaCodeSandboxTemplate;
import com.wangheng.wojcodesandbox.util.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) throws IOException, InterruptedException {
        List<ExecuteMessage> runMessageList=new ArrayList<>();
        String userCodeParentPath=userCodeFile.getParentFile().getAbsolutePath();
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        /**
         * 创建容器
         */
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd("openjdk:8-alpine");
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L); //100M
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
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        final long[] memory = {0L};
        statsCmd.exec(new ResultCallback<Statistics>() {
            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onNext(Statistics statistics) {
                System.out.println("运行内存："+statistics.getMemoryStats().getUsage());
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
        for (String inputArgs : inputList) {
            ExecuteMessage executeMessage=new ExecuteMessage();
            memory[0]=0L;
            StopWatch stopWatch=new StopWatch();
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"},inputArgs.split(" "));
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
                        sbErr.append(err);
                    } else {
                        String out=new String(frame.getPayload());
                        System.out.println("输出结果：" + out);
                        sbOut.append(out);
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
            runMessageList.add(executeMessage);
        }
        statsCmd.close();

        /**
         * 删除容器
         */
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        System.out.printf("容器%s删除成功\n",containerId);

        return runMessageList;
    }
}
