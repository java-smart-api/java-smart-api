package com.citrsw.controller;

import com.citrsw.core.ApiContext;
import com.citrsw.definition.ApiProject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * 写点注释
 *
 * @author Zhenfeng Li
 * @version 1.0
 * @date 2020-01-13 20:19
 */
@RestController
public class ApiController {

    private final ApiContext apiContext;

    public ApiController(ApiContext apiContext) {
        this.apiContext = apiContext;
    }

    @GetMapping("/api")
    public ApiProject api(String group) {
        Map<String, ApiProject> apiProjectMap = apiContext.getApiProjectMap();
        if (StringUtils.isBlank(group)) {
            for (ApiProject value : apiProjectMap.values()) {
                return value;
            }
        }
        return apiProjectMap.get(group);
    }

    @GetMapping("/api/spaces")
    public Set<String> spaces() {
        Map<String, ApiProject> apiProjectMap = apiContext.getApiProjectMap();
        return apiProjectMap.keySet();
    }
}
