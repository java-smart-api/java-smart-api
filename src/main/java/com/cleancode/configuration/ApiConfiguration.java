package com.cleancode.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置类
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 09:11:09
 */
@Configuration
@ComponentScan({"com.cleancode.controller", "com.cleancode.core"})
public class ApiConfiguration implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/api/");
        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/api/");
        registry.addResourceHandler("/smart/api/**").addResourceLocations("classpath:/api/");
    }
}
