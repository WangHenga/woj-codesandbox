package com.wangheng.wojcodesandbox.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCodeRequest implements Serializable {
    private List<String> inputList;
    private String language;
    private String code;
    private static final long serialVersionUID = 1L;
}
