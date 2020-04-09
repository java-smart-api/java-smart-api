package com.cleancode.definition;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API类定义
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 11:46:02
 */
@Data
public class ApiClass implements Serializable {
    private static final long serialVersionUID = 1756291779709228363L;

    /**
     * 类别名
     */
    private String name;

    /**
     * 类名
     */
    private String className;

    /**
     * 全类名
     */
    private String fullClassName;

    /**
     * 描述
     */
    private String description;

    /**
     * 方法集合
     */
    private List<ApiMethod> apiMethods;

    public List<ApiMethod> getApiMethods() {
        return apiMethods == null || apiMethods.size() == 0 ? null : apiMethods.stream().sorted(Comparator.comparing(ApiMethod::getName)).collect(Collectors.toCollection(ArrayList::new));
    }
}
