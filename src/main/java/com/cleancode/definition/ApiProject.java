package com.cleancode.definition;

import lombok.Data;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API配置
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 11:36:56
 */
@Data
public class ApiProject implements Serializable {
    private static final long serialVersionUID = 4975160821774544353L;

    /**
     * 所属空间
     */
    private String space;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 开发员网站
     */
    private String website;

    /**
     * 开发员邮箱
     */
    private String email;

    /**
     * url是否加项目名
     */
    private Boolean projectName;

    /**
     * 所有的类
     */
    private Set<ApiClass> classList = new HashSet<>();

    public Set<ApiClass> getClassList() {
        return classList == null || classList.size() == 0 ? null : classList.stream().sorted(Comparator.comparing(ApiClass::getName)).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
