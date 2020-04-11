package com.citrsw.annatation;

import java.lang.annotation.*;

/**
 * 请求方法注解
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 11:25:37
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ApiOperation {
    /**
     * 名称
     */
    String name() default "";

    /**
     * 描述
     */
    String description() default "";

    /**
     * 如果返回java基本对象则需要设置此属性
     */
    String responseDescription() default "";

}
