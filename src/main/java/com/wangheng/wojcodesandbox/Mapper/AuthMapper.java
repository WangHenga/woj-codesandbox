package com.wangheng.wojcodesandbox.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthMapper {
    @Select("select app_secret from auths where app_key=#{appkey}")
    String getAppSecretByAppKey(String appKey);
}
