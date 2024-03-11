package com.wangheng.wojcodesandbox.util;

import lombok.Data;

@Data
public class ExecuteMessage {
    private Integer exitValue;
    private String output;
    private String ErrorMessage;
    private Long time;
    private Long memory;
}
