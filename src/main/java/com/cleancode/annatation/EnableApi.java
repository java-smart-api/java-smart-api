package com.cleancode.annatation;

import com.cleancode.configuration.ApiConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 激活注解
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 09:29:15
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ApiConfiguration.class})
public @interface EnableApi {
}
