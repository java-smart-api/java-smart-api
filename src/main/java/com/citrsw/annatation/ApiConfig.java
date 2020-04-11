package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * Api配置单个
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 09:46:11
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiConfig {
    /**
     * 所属空间
     */
    String space() default "default";

    /**
     * 标题
     */
    String title() default "";

    /**
     * 描述
     */
    String description() default "";

    /**
     * 开发员网站
     */
    String website() default "";

    /**
     * 开发员邮箱
     */
    String email() default "";

    /**
     * url是否加项目名
     */
    boolean projectName() default true;

    /**
     * 单个包配置
     */
    String basePackage() default "";

    /**
     * 多个包配置
     */
    String[] basePackages() default {};
}
