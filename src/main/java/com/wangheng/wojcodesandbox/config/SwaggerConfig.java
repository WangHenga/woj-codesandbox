package com.wangheng.wojcodesandbox.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * swagger接口文档的配置
 */
@Configuration
public class SwaggerConfig extends WebMvcConfigurationSupport {

    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                // 分组名称
                .groupName("代码沙箱接口测试文档")
                .select()
                //这里标注控制器的位置
                .apis(RequestHandlerSelectors.basePackage("com.wangheng.wojcodesandbox.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * api信息
     * @return api对象信息
     */
    private ApiInfo apiInfo()   {
        return new ApiInfoBuilder()
                .title("代码沙箱接口文档")
                .version("1.0")
                .description("代码沙箱接口文档")
                .build();
    }
}
