package com.wangheng.wojcodesandbox.docker;

import com.wangheng.wojcodesandbox.codesandbox.impl.JavaDockerCodeSandboxOld;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeRequest;
import com.wangheng.wojcodesandbox.codesandbox.model.ExecuteCodeResponse;

import java.io.*;
import java.util.Arrays;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException, IOException {
//        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
//        /**
//         * 创建容器
//         */
//        CreateContainerCmd containerCmd = dockerClient.createContainerCmd("openjdk:8-alpine");
//        HostConfig hostConfig = new HostConfig();
//        hostConfig.withMemory(100 * 1000 * 1000L);
//        hostConfig.withMemorySwap(0L);
//        hostConfig.withCpuCount(1L);
//        hostConfig.setBinds(new Bind("/home/woj-codesandbox/tmpFile", new Volume("/app")));
//        CreateContainerResponse createContainerResponse = containerCmd
//                .withHostConfig(hostConfig)
//                .withNetworkDisabled(true)
//                .withAttachStdin(true)
//                .withAttachStderr(true)
//                .withAttachStdout(true)
//                .withTty(true)
//                .exec();
//        System.out.println(createContainerResponse);
//        String containerId = createContainerResponse.getId();
//
//
//        dockerClient.startContainerCmd(containerId).exec();
//
//        String[] cmdArray =new String[]{"java", "-cp", "/app", "Main","1","2"};
//        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
//                .withCmd(cmdArray)
//                .withAttachStderr(true)
//                .withAttachStdin(true)
//                .withAttachStdout(true)
//                .exec();
//        System.out.println("创建执行命令：" + execCreateCmdResponse);
//
//        ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
//            @Override
//            public void onNext(Frame frame) {
//                StreamType streamType = frame.getStreamType();
//                if (StreamType.STDERR.equals(streamType)) {
//                    System.out.println("输出错误结果：" + new String(frame.getPayload()));
//                } else {
//                    System.out.println("输出结果：" + new String(frame.getPayload()));
//                }
//                super.onNext(frame);
//            }
//        };
//        boolean b = dockerClient.execStartCmd(execCreateCmdResponse.getId())
//                .exec(execStartResultCallback)
//                .awaitCompletion(5L, TimeUnit.SECONDS);
//        System.out.println("是否超时："+b);
//
//        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest(Arrays.asList("1 2", "3 3"), "java",
                "public class Main{\n" +
                        "\tpublic static void main(String[] args){\n" +
                        "\t\tint a=Integer.parseInt(args[0]);\n" +
                        "\t\tint b=Integer.parseInt(args[1]);\n" +
                        "\t\tSystem.out.println(a+b);\n" +
                        "\t}\n" +
                        "}");
        JavaDockerCodeSandboxOld javaDockerCodeSandbox = new JavaDockerCodeSandboxOld();
        ExecuteCodeResponse response = javaDockerCodeSandbox.doExecute(executeCodeRequest);
        System.out.println(response);
    }
}
