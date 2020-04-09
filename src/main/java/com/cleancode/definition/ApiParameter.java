package com.cleancode.definition;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Api参数属性定义
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 11:58:11
 */
@Data
public class ApiParameter implements Serializable {
    private static final long serialVersionUID = 3302751023052603486L;

    /**
     * 别称
     */
    private String name;

    /**
     * 参数java类型
     */
    private String typeFullName;

    /**
     * 类型
     */
    private String type;

    /**
     * 是否数组
     */
    private Boolean isArray = false;

    /**
     * 是否必须
     */
    private Boolean require = false;

    /**
     * 描述
     */
    private String description;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 示例
     */
    private String example;

    /**
     * 子类型集合
     */
    private Set<ApiParameter> apiParameters;

    /**
     * 驼峰命名是否转下划线
     */
    private Boolean underline = true;

    public ApiParameter() {
    }

    public String getUnderlineName() {
        //如果是驼峰转下划线则转换
        if (StringUtils.isNotBlank(name)) {
            StringBuilder sb = new StringBuilder(name);
            //定位
            int temp = 0;
            for (int i = 0; i < name.length(); i++) {
                if (Character.isUpperCase(name.charAt(i))) {
                    sb.insert(i + temp, "_");
                    temp += 1;
                }
            }
            String lowerCase = sb.toString().toLowerCase();
            return lowerCase.startsWith("_") ? lowerCase.replaceFirst("_", "") : lowerCase;
        }
        return name;
    }

    /**
     * 子类中油必须的参数则父类为必须
     */
    public Boolean getRequire() {
        if (!require && apiParameters != null) {
            for (ApiParameter apiParameter : apiParameters) {
                Boolean require = apiParameter.getRequire();
                if (require) {
                    return true;
                }
            }
        }
        return require;
    }

    public String getType() {
        if (StringUtils.isNotBlank(type)) {
            return type;
        }
        String typeName = this.typeFullName;
        if (StringUtils.isBlank(typeName)) {
            return "";
        }
        if (typeName.contains("[")) {
            typeName = typeName.substring(0, typeName.indexOf("["));
            isArray = true;
            if (StringUtils.isNotBlank(name)) {
                if (!name.contains("[0]")) {
                    return "";
                }
            } else {
                return "";
            }
        }
        String type = "";
        switch (typeName) {
            case "java.lang.Long":
                type = "long";
                break;
            case "java.lang.String":
                type = "string";
                break;
            case "java.lang.Integer":
                type = "int";
                break;
            case "java.time.LocalDateTime":
                type = "datetime";
                break;
            case "java.time.LocalDate":
                type = "date";
                break;
            case "java.time.LocalTime":
                type = "time";
                break;
            case "java.lang.Boolean":
                type = "boolean";
                break;
            case "java.util.Set":
            case "java.util.List":
                isArray = true;
                break;
            default:
                type = "";
        }
        return type;
    }

    public Set<ApiParameter> getApiParameters() {
        return apiParameters == null || apiParameters.size() == 0 ? null : apiParameters.stream().sorted(Comparator.comparing(ApiParameter::getName)).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
