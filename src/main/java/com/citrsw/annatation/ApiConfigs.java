package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * Api配置多个
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 09:46:11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiConfigs {
    /**
     * Api配置多个
     */
    ApiConfig[] apiConfig();

    /**
     * 是否驼峰转下划线
     */
    boolean underline() default false;
}
