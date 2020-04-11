package com.citrsw.definition;

import lombok.Data;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 方法定义
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 11:54:14
 */
@Data
public class ApiMethod implements Serializable {
    private static final long serialVersionUID = 1913972444402902193L;

    /**
     * 请求方式
     */
    private String mode;

    /**
     * 方法名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 请求路径
     */
    private String url;

    /**
     * 入参是否是json
     */
    private Boolean inJson;

    /**
     * 出参是否是json
     */
    private Boolean outJson = true;

    /**
     * 参数是否必须
     */
    private Boolean require;

    /**
     * Json入参属性集合
     */
    private ApiParameter inJsonApiParameter;

    /**
     * from-data入参属性集合
     */
    private Set<ApiParameter> inFormDataApiParameters;

    /**
     * 出参属性集合
     */
    private ApiParameter outApiParameter;

    public Set<ApiParameter> getInFormDataApiParameters() {
        return inFormDataApiParameters == null || inFormDataApiParameters.size() == 0 ? null : inFormDataApiParameters.stream().sorted(Comparator.comparing(ApiParameter::getName)).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
