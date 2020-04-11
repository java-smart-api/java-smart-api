package com.citrsw.annatation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义属性注解
 * 主要对象形式的入参
 * 将多余的属性排除，增加Api入参的可读性
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-01-12 23:04
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiModelProperty {

    /**
     * 必须属性
     * 表示必须要传值的属性
     */
    String[] require() default {};

    /**
     * 非必须属性
     * 表示必须非要传值的属性
     */
    String[] nonRequire() default {};
}
