package com.cleancode.annatation;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Model属性注解
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 19:53:31
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiProperty {
    /**
     * 属性名称
     */
    String name() default "";

    /**
     * 描述
     */
    String description() default "";

    /**
     * 属性类型
     */
    String type() default "";

    /**
     * 是否必须
     */
    boolean required() default false;

    /**
     * 是否显示
     */
    boolean hidden() default false;

    /**
     * 默认值
     */
    String defaultValue() default ValueConstants.DEFAULT_NONE;

    /**
     * 示例
     */
    String example() default "";
}
