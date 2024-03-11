package com.wangheng.wojcodesandbox.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessUtils {
    public static ExecuteMessage runProcessAndGetMessage(String cmd,String opName) throws IOException, InterruptedException {
        StopWatch stopWatch=new StopWatch();
        ExecuteMessage executeMessage = new ExecuteMessage();
        Process compileProcess = Runtime.getRuntime().exec(cmd);
        stopWatch.start();
        boolean isFinish = compileProcess.waitFor(10L, TimeUnit.SECONDS);
        stopWatch.stop();
        if(isFinish){
            int exitValue=compileProcess.exitValue();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                /**
                 * 命令执行成功
                 */
                log.info("{}成功",opName);
                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                executeMessage.setOutput(StringUtils.join(outputStrList, '\n'));
            } else {
                /**
                 * 命令执行失败
                 */
                log.info("{}失败,退出码：{}", opName,exitValue);
                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                executeMessage.setOutput(StringUtils.join(outputStrList, '\n'));
                // 分批获取进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream(), StandardCharsets.UTF_8));
                // 逐行读取
                List<String> errorOutputStrList = new ArrayList<>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorOutputStrList.add(errorCompileOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(errorOutputStrList, '\n'));
            }
        }else{
            log.error("{}失败，运行超时",opName);
            executeMessage.setErrorMessage(String.format("%s失败，运行超时",opName));
        }
        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        return executeMessage;
    }
    public static ExecuteMessage runInteractProcessAndGetMessage(String cmd,String opName,String inputStr) throws IOException, InterruptedException {
        StopWatch stopWatch=new StopWatch();
        ExecuteMessage executeMessage = new ExecuteMessage();
        Process compileProcess = Runtime.getRuntime().exec(cmd);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(compileProcess.getOutputStream());
        outputStreamWriter.write(inputStr+"\n");
        outputStreamWriter.flush();
        stopWatch.start();
        boolean isFinish = compileProcess.waitFor(10L,TimeUnit.SECONDS);
        stopWatch.stop();
        if (isFinish) {
            /**
             *命令执行成功
             */
            int exitValue = compileProcess.exitValue();
            executeMessage.setExitValue(exitValue);

            if(exitValue==0) {
                log.info("{}成功", opName);
                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                executeMessage.setOutput(StringUtils.join(outputStrList, '\n'));
            }else{
                /**
                 * 命令执行失败
                 */
                log.error("{}失败,退出码：{}", opName,exitValue);
                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                executeMessage.setOutput(StringUtils.join(outputStrList, '\n'));
                // 分批获取进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                // 逐行读取
                List<String> errorOutputStrList = new ArrayList<>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorOutputStrList.add(errorCompileOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(errorOutputStrList, '\n'));
            }

        } else {
            log.error("{}失败，运行超时", opName);
            executeMessage.setErrorMessage(String.format("%s失败，运行超时", opName));
        }
        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        return executeMessage;
    }
}
