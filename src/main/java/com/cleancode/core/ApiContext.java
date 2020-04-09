package com.cleancode.core;

import com.alibaba.fastjson.JSON;
import com.cleancode.annatation.*;
import com.cleancode.definition.ApiClass;
import com.cleancode.definition.ApiMethod;
import com.cleancode.definition.ApiParameter;
import com.cleancode.definition.ApiProject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 上下文(核心)
 *
 * @author Zhenfeng Li
 * @date 2020-01-10 09:25:21
 */
@Slf4j
@Component
public class ApiContext {
    @Value("${spring.application.name:}")
    private String name;

    /**
     * 是否转下划线
     */
    private Boolean underline;

    /**
     * JDK基本类型集合
     */
    private Set<String> basicTypes = new HashSet<>(Arrays.asList(Date.class.getName(), LocalDate.class.getName(),
            LocalDateTime.class.getName(), LocalTime.class.getName(),
            Boolean.class.getName(), Long.class.getName(), Integer.class.getName(), Double.class.getName(), Float.class.getName(),
            boolean.class.getName(), long.class.getName(), int.class.getName(), double.class.getName(), float.class.getName(),
            String.class.getName()));

    /**
     * Controller包分空间存储
     */
    private Map<String, Set<String>> spaceScanMap = new HashMap<>();
    private Map<String, Set<ApiClass>> spaceApiClassMap = new ConcurrentHashMap<>();

    /**
     *
     */
    private Set<ApiProject> apiProjects = new HashSet<>();
    private Map<String, ApiProject> apiProjectMap = new ConcurrentHashMap<>();

    public Set<ApiProject> getApiProjects() {
        return apiProjects;
    }

    public Map<String, ApiProject> getApiProjectMap() {
        return apiProjectMap;
    }

    /**
     * 启动类
     */
    private Class<?> mainApplicationClass;

    @PostConstruct
    public void init() {
        try {
            StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if ("main".equals(stackTraceElement.getMethodName())) {
                    mainApplicationClass = Class.forName(stackTraceElement.getClassName());
                    getScannerDir();
                    scannerClass();
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        for (ApiProject apiProject : apiProjects) {
            apiProject.setName(name.trim());
            apiProject.setClassList(spaceApiClassMap.get(apiProject.getSpace()));
            apiProjectMap.put(apiProject.getSpace(), apiProject);
        }
    }

    /**
     * 获取扫描目录
     */
    private void getScannerDir() {
        //获取配置信息
        ApiConfigs apiConfigs = mainApplicationClass.getAnnotation(ApiConfigs.class);
        if (apiConfigs != null) {
            underline = apiConfigs.underline();
            ApiConfig[] apiConfigsArray = apiConfigs.apiConfig();
            for (ApiConfig apiConfig : apiConfigsArray) {
                if (apiConfig != null) {
                    String title = apiConfig.title();
                    String space = apiConfig.space();
                    String description = apiConfig.description();
                    String website = apiConfig.website();
                    boolean projectName = apiConfig.projectName();
                    String email = apiConfig.email();
                    ApiProject apiProject = new ApiProject();
                    apiProject.setSpace(space);
                    apiProject.setDescription(description);
                    apiProject.setEmail(email);
                    apiProject.setProjectName(projectName);
                    apiProject.setTitle(title);
                    apiProject.setWebsite(website);
                    String basePackage = apiConfig.basePackage();
                    String[] strings = apiConfig.basePackages();
                    //获取配置扫描包
                    Set<String> scannerSet = new HashSet<>();
                    if (!StringUtils.isBlank(basePackage)) {
                        scannerSet.add(basePackage);
                    }
                    //如果没有配置则默认从启动类下开始扫描
                    if (scannerSet.isEmpty()) {
                        scannerSet.add(mainApplicationClass.getPackage().getName());
                    }
                    scannerSet.addAll(Arrays.asList(strings));
                    spaceScanMap.put(space, scannerSet);
                    apiProjects.add(apiProject);
                }
            }
        } else {
            Set<String> scannerSet = new HashSet<>();
            //如果没有配置则默认从启动类下开始扫描
            scannerSet.add(mainApplicationClass.getPackage().getName());
            spaceScanMap.put("default", scannerSet);
        }

    }

    /**
     * 扫描
     */
    private void scannerClass() {
        for (Map.Entry<String, Set<String>> entry : spaceScanMap.entrySet()) {
            Set<ApiClass> apiClasses = new HashSet<>();
            spaceApiClassMap.put(entry.getKey(), apiClasses);
            for (String scanner : entry.getValue()) {
                scannerClass(scanner, apiClasses);
            }
        }

    }

    /**
     * 递归扫描类
     *
     * @param scanner 目录名
     */
    private void scannerClass(String scanner, Set<ApiClass> apiClasses) {
        String s = scanner.replace(".", "/");
        URL url = ApiContext.class.getClassLoader().getResource(s);
        // 获取包的名字 并进行替换
        String packageDirName = scanner.replace('.', '/');
        assert url != null;
        // 得到协议的名称
        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            File[] files = new File(url.getFile()).listFiles();
            assert files != null;
            for (File classFile : files) {
                if (classFile.isDirectory()) {
                    scannerClass(scanner + "." + classFile.getName(), apiClasses);
                    continue;
                }
                String path = classFile.getPath();
                if (path.endsWith(".class")) {
                    Class<?> clazz = getClazz(scanner + "." + classFile.getName().replace(".class", ""));
                    if (Objects.nonNull(clazz)) {
                        if (clazz.isAnnotationPresent(Controller.class) && !clazz.isAnnotationPresent(ApiIgnore.class)) {
                            ApiClass apiClass = createClassApi(clazz);
                            List<ApiMethod> methodApi = createMethodApi(clazz, false);
                            //组织类和方法
                            apiClass.setApiMethods(methodApi);
                            apiClasses.add(apiClass);
                        } else if (clazz.isAnnotationPresent(RestController.class) && !clazz.isAnnotationPresent(ApiIgnore.class)) {
                            ApiClass apiClass = createClassApi(clazz);
                            List<ApiMethod> methodApi = createMethodApi(clazz, true);
                            //组织类和方法
                            apiClass.setApiMethods(methodApi);
                            apiClasses.add(apiClass);
                        }
                    }
                }
            }
        } else if ("jar".equals(protocol)) {
            // 如果是jar包文件
            try {
                // 获取jar
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                // 从此jar包 得到一个枚举类
                Enumeration<JarEntry> entries = jar.entries();
                // 同样的进行循环迭代
                while (entries.hasMoreElements()) {
                    // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    // 如果是以/开头的
                    if (name.charAt(0) == '/') {
                        // 获取后面的字符串
                        name = name.substring(1);
                    }
                    // 如果前半部分和定义的包名相同
                    if (name.startsWith(packageDirName)) {
                        // 如果是一个.class文件 而且不是目录
                        if (name.endsWith(".class") && !entry.isDirectory()) {
                            Class<?> clazz = getClazz(name.replace("/", ".").replace("\\", ".")
                                    .replace(".class", ""));
                            if (Objects.nonNull(clazz)) {
                                if (clazz.isAnnotationPresent(Controller.class) && !clazz.isAnnotationPresent(ApiIgnore.class)) {
                                    ApiClass apiClass = createClassApi(clazz);
                                    List<ApiMethod> methodApi = createMethodApi(clazz, false);
                                    //组织类和方法
                                    apiClass.setApiMethods(methodApi);
                                    apiClasses.add(apiClass);
                                } else if (clazz.isAnnotationPresent(RestController.class) && !clazz.isAnnotationPresent(ApiIgnore.class)) {
                                    ApiClass apiClass = createClassApi(clazz);
                                    List<ApiMethod> methodApi = createMethodApi(clazz, true);
                                    //组织类和方法
                                    apiClass.setApiMethods(methodApi);
                                    apiClasses.add(apiClass);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 创建类的定义
     *
     * @param beanClassName 类全名
     * @return 返回类的定义
     */
    private Class<?> getClazz(String beanClassName) {
        try {
            return Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建类Api
     */
    private ApiClass createClassApi(Class<?> clazz) {
        Api api = clazz.getAnnotation(Api.class);
        ApiClass apiClass = new ApiClass();
        apiClass.setName(clazz.getSimpleName());
        apiClass.setFullClassName(clazz.getName());
        apiClass.setClassName(clazz.getSimpleName());
        if (api != null) {
            String name = api.name();
            String description = api.description();
            if (StringUtils.isNotBlank(name)) {
                apiClass.setName(name);
            }
            apiClass.setDescription(description);
        }
        return apiClass;
    }


    /**
     * 创建方法Api
     */
    private List<ApiMethod> createMethodApi(Class<?> clazz, boolean isRestController) {
        Method[] methods = clazz.getMethods();
        RequestMapping classRequestMapping = clazz.getAnnotation(RequestMapping.class);
        List<ApiMethod> apiMethods = new ArrayList<>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(ApiIgnore.class)) {
                continue;
            }
            String[] baseUrls;
            if (classRequestMapping != null) {
                baseUrls = classRequestMapping.value();
            } else {
                baseUrls = new String[]{""};
            }
            for (String baseUrl : baseUrls) {
                ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                GetMapping getMapping = method.getAnnotation(GetMapping.class);
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                PutMapping putMapping = method.getAnnotation(PutMapping.class);
                DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
                boolean condition = (isRestController && (requestMapping != null || getMapping != null || postMapping != null || putMapping != null || deleteMapping != null)) ||
                        (responseBody != null && (requestMapping != null || getMapping != null || postMapping != null || putMapping != null || deleteMapping != null));
                if (condition) {
                    ApiMethod apiMethod = new ApiMethod();
                    apiMethod.setUrl(baseUrl);
                    apiMethod.setName(method.getName());
                    if (apiOperation != null) {
                        String name = apiOperation.name();
                        String description = apiOperation.description();
                        if (StringUtils.isNotBlank(name)) {
                            apiMethod.setName(name);
                        }
                        apiMethod.setDescription(description);
                    }

                    //入参
                    createMethodsInParameter(method, apiMethod);
                    //出参
                    createMethodsOutParameter(method, apiMethod);
                    //处理请求形式
                    if (getMapping != null) {
                        apiMethod.setMode("GET");
                        cloneMethod(apiMethods, apiMethod, getMapping.value());
                    } else if (postMapping != null) {
                        apiMethod.setMode("POST");
                        cloneMethod(apiMethods, apiMethod, postMapping.value());
                    } else if (putMapping != null) {
                        apiMethod.setMode("PUT");
                        cloneMethod(apiMethods, apiMethod, putMapping.value());
                    } else if (deleteMapping != null) {
                        apiMethod.setMode("DELETE");
                        cloneMethod(apiMethods, apiMethod, deleteMapping.value());

                    } else if (requestMapping != null) {
                        RequestMethod[] requestMethods = requestMapping.method();
                        String[] values = requestMapping.value();
                        if (values.length == 0) {
                            continue;
                        }
                        for (String value : values) {
                            if (requestMethods.length > 0) {
                                for (RequestMethod requestMethod : requestMethods) {
                                    ApiMethod cloneMethod = JSON.parseObject(JSON.toJSONString(apiMethod), ApiMethod.class);
                                    cloneMethod.setMode(requestMethod.name());
                                    apiMethods.add(cloneMethod);
                                }
                            } else {
                                ApiMethod cloneMethod = JSON.parseObject(JSON.toJSONString(apiMethod), ApiMethod.class);
                                cloneMethod.setUrl(apiMethod.getUrl() + "/" + value);
                                cloneMethod.setMode("GET");
                                apiMethods.add(cloneMethod);
                                cloneMethod = JSON.parseObject(JSON.toJSONString(apiMethod), ApiMethod.class);
                                cloneMethod.setUrl(apiMethod.getUrl() + "/" + value);
                                cloneMethod.setMode("POST");
                                apiMethods.add(apiMethod);
                                cloneMethod = JSON.parseObject(JSON.toJSONString(apiMethod), ApiMethod.class);
                                cloneMethod.setUrl(apiMethod.getUrl() + "/" + value);
                                cloneMethod.setMode("PUT");
                                apiMethods.add(apiMethod);
                                cloneMethod = JSON.parseObject(JSON.toJSONString(apiMethod), ApiMethod.class);
                                cloneMethod.setUrl(apiMethod.getUrl() + "/" + value);
                                cloneMethod.setMode("DELETE");
                                apiMethods.add(apiMethod);
                            }
                        }
                    }
                }
            }
        }
        return apiMethods;
    }

    /**
     * 克隆方法
     */
    private void cloneMethod(List<ApiMethod> apiMethods, ApiMethod apiMethod, String[] values) {
        if (values.length == 0) {
            return;
        }
        for (String value : values) {
            ApiMethod cloneMethod = JSON.parseObject(JSON.toJSONString(apiMethod), ApiMethod.class);
            cloneMethod.setUrl(apiMethod.getUrl() + "/" + value);
            apiMethods.add(cloneMethod);
        }
    }

    /**
     * 创建入参Api
     */
    public void createMethodsInParameter(Method method, ApiMethod apiMethod) {
        Set<ApiParameter> apiParameters = new HashSet<>();
        //自定义入参对象属性
        ApiModelProperty apiModelProperty = method.getAnnotation(ApiModelProperty.class);
        Map<String, Boolean> propertyMap = new HashMap<>();
        if (apiModelProperty != null) {
            String[] requires = apiModelProperty.require();
            String[] nonRequires = apiModelProperty.nonRequire();
            for (String nonRequire : nonRequires) {
                propertyMap.put(nonRequire, false);
            }
            //如果必须和非必须的同时配置了相同的属性，则必须覆盖非必须
            for (String require : requires) {
                propertyMap.put(require, true);
            }
        }
        //jdk8以上开始支持
        Parameter[] params = method.getParameters();
        for (Parameter param : params) {
            ApiIgnore apiIgnore = param.getAnnotation(ApiIgnore.class);
            if (apiIgnore != null) {
                continue;
            }
            ApiParameter apiParameter = new ApiParameter();
            apiParameter.setUnderline(underline);
            apiParameter.setName(param.getName());
            apiParameter.setTypeFullName(param.getType().getName());
            RequestBody requestBody = param.getAnnotation(RequestBody.class);
            ApiParam apiParam = param.getAnnotation(ApiParam.class);
            if (requestBody == null && apiParam != null) {
                String name = apiParam.name();
                boolean require = apiParam.require();
                String description = apiParam.description();
                String defaultValue = apiParam.defaultValue();
                if (StringUtils.isNotBlank(name)) {
                    apiParameter.setUnderline(underline);
                    apiParameter.setName(name);
                }
                apiParameter.setRequire(require);
                apiParameter.setDescription(description);
                apiParameter.setDefaultValue(StringUtils.equals(ValueConstants.DEFAULT_NONE, defaultValue) ? null : defaultValue);
            }
            if (requestBody != null) {
                //json形式的入参
                apiMethod.setInJson(true);
                //json不支持基本类型参数
                if (filterBastType(param.getType().getName())) {
                    return;
                }
                //json不支持数组和集合形式的参数
                if (Collection.class.isAssignableFrom(param.getType()) || param.getType().isArray()) {
                    return;
                }
                //json形式解析类模型注解
                ApiModel apiModel = param.getType().getAnnotation(ApiModel.class);
                if (apiModel != null) {
                    String name = apiModel.name();
                    if (StringUtils.isNotBlank(name)) {
                        apiParameter.setUnderline(underline);
                        apiParameter.setName(name);
                    }
                    String description = apiModel.description();
                    apiParameter.setDescription(description);
                }
                //处理基本类型参数
                if (filterBastType(param.getType().getName())) {
                    apiParameters.add(apiParameter);
                    continue;
                }
                //用于循环依赖判定
                Set<Class<?>> cyclicProperty = new HashSet<>();
                cyclicProperty.add(param.getType());
                apiParameter.setApiParameters(createJsonParameter(param.getType(), null, "set", cyclicProperty, propertyMap));
                apiParameter.setRequire(true);
                apiMethod.setInJsonApiParameter(apiParameter);
                //json参数只能有一个
                return;
            }
            //form-data形式的入参
            //处理基本类型参数
            if (filterBastType(param.getType().getName())) {
                apiParameters.add(apiParameter);
                continue;
            }
            //用于循环依赖判定
            Set<Class<?>> cyclicProperty = new HashSet<>();
            //集合
            if (Collection.class.isAssignableFrom(param.getType())) {
                apiParameter.setTypeFullName(param.getType().getName());
                apiParameter.setUnderline(underline);
                apiParameter.setName(apiParameter.getName() + "[0]");
                getFormDataPropertyType(apiParameters, param.getParameterizedType(), apiParameter, apiParameter.getName() + "[0]", "set", cyclicProperty, propertyMap);
            } else if (param.getType().isArray()) {
                //数组
                apiParameter.setTypeFullName(param.getType().getCanonicalName());
                apiParameter.setUnderline(underline);
                apiParameter.setName(apiParameter.getName() + "[0]");
                getFormDataPropertyType(apiParameters, (param.getType()).getComponentType(), apiParameter, apiParameter.getName() + "[0]", "set", cyclicProperty, propertyMap);
            }
            //加入到循环依赖Set中
            if (!filterBastType(param.getType().getName())) {
                //先把自己加进去
                cyclicProperty.add(param.getType());
            }


            //处理对象类型参数
            apiParameters.addAll(createFormDataParameter(param.getType(), apiParameter, "", "set", cyclicProperty, propertyMap));
        }
        //到这里表示肯定是form-data形式
        apiMethod.setInJson(false);
        apiMethod.setInFormDataApiParameters(apiParameters);
    }

    /**
     * 创建form-data形式属性Api
     */
    public Set<ApiParameter> createFormDataParameter(Class<?> clazz, ApiParameter parentApiParameter, String parentName, String prefix, Set<Class<?>> cyclicProperty, Map<String, Boolean> propertyMap) {
        Method[] methods = clazz.getMethods();
        Set<ApiParameter> apiParameters = new HashSet<>();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith(prefix)) {
                methodName = StringUtils.uncapitalize(methodName.replaceFirst(prefix, ""));
            }
            ApiParameter apiParameter = new ApiParameter();
            try {
                //遍历属性
                Field field = clazz.getDeclaredField(methodName);
                apiParameter.setUnderline(underline);
                apiParameter.setName(StringUtils.isNotBlank(parentName) ? parentName + "." + field.getName() : field.getName());
                apiParameter.setTypeFullName(field.getType().getName());
                ApiProperty apiProperty = field.getAnnotation(ApiProperty.class);
                if (apiProperty != null) {
                    if (apiProperty.hidden()) {
                        continue;
                    }
                    String name = apiProperty.name();
                    String example = apiProperty.example();
                    String description = apiProperty.description();
                    String type = apiProperty.type();
                    boolean required = apiProperty.required();
                    String defaultValue = apiProperty.defaultValue();
                    if (StringUtils.isNotBlank(name)) {
                        apiParameter.setUnderline(underline);
                        apiParameter.setName(StringUtils.isNotBlank(parentName) ? parentName + "." + name : name);
                    }
                    apiParameter.setDescription(description);
                    if (!StringUtils.equals(ValueConstants.DEFAULT_NONE, defaultValue)) {
                        apiParameter.setDefaultValue(defaultValue);
                    }
                    if (StringUtils.isNotBlank(type)) {
                        apiParameter.setType(type);
                    }
                    apiParameter.setRequire(required);
                    apiParameter.setExample(example);
                }
                if (!propertyMap.isEmpty()) {
                    if (!propertyMap.containsKey(apiParameter.getName())) {
                        continue;
                    }
                    apiParameter.setRequire(propertyMap.get(apiParameter.getName()));
//                    propertyMap.remove(apiParameter.getName());
                }
                //处理集合
                if (Collection.class.isAssignableFrom(field.getType())) {
                    apiParameter.setTypeFullName(field.getType().getName());
                    getFormDataPropertyType(apiParameters, field.getGenericType(), parentApiParameter, apiParameter.getName() + "[0]", "set", cyclicProperty, propertyMap);
                } else if (field.getType().isArray()) {
                    apiParameter.setTypeFullName(field.getType().getCanonicalName());
                    getFormDataPropertyType(apiParameters, (field.getType()).getComponentType(), parentApiParameter, apiParameter.getName() + "[0]", prefix, cyclicProperty, propertyMap);
                } else {
                    //递归创建子参数
                    //处理基本类型参数
                    if (filterBastType(field.getType().getName())) {
                        apiParameters.add(apiParameter);
                        //最终为基本类型时结束
                        continue;
                    }
                    if (cyclicProperty.contains(field.getType())) {
                        //发现循环依赖则直接进入下一循环
                        continue;
                    }
                    //加入到循环依赖Set中
                    if (!filterBastType(field.getType().getName())) {
                        cyclicProperty.add(field.getType());
                    }
                    apiParameters.addAll(createFormDataParameter(field.getType(), apiParameter, apiParameter.getName(), "set", cyclicProperty, propertyMap));
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        return apiParameters;
    }

    /**
     * 处理集合泛型form-data
     */
    public void getFormDataPropertyType(Set<ApiParameter> apiParameters, Type gType, ApiParameter apiParameter, String parentName, String prefix, Set<Class<?>> cyclicProperty, Map<String, Boolean> propertyMap) {
        // 如果gType类型是ParameterizedType对象
        if (gType instanceof ParameterizedType) {
            // 强制类型转换
            ParameterizedType pType = (ParameterizedType) gType;
            // 取得泛型类型的泛型参数
            Type[] tArgs = pType.getActualTypeArguments();
            try {
                if (tArgs[0] instanceof Class && ((Class<?>) tArgs[0]).isArray()) {
                    apiParameter.setUnderline(underline);
                    apiParameter.setName(apiParameter.getName() + "[0]");
                    getFormDataPropertyType(apiParameters, ((Class<?>) tArgs[0]).getComponentType(), apiParameter, apiParameter.getName() + "[0]", prefix, cyclicProperty, propertyMap);
                } else if (Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) tArgs[0]).getRawType())) {
                    apiParameter.setUnderline(underline);
                    apiParameter.setName(apiParameter.getName() + "[0]");
                    getFormDataPropertyType(apiParameters, tArgs[0], apiParameter, parentName + "[0]", prefix, cyclicProperty, propertyMap);
                }
            } catch (ClassCastException e) {
                Class<?> clazz = (Class<?>) tArgs[0];
                //如果最终为基本数据类型则不再解析
                if (filterBastType(clazz.getName())) {
                    apiParameters.add(apiParameter);
                    return;
                }
                if (cyclicProperty.contains(clazz)) {
                    //发现循环依赖则直接进入下一循环
                    return;
                }
                //加入到循环依赖Set中
                if (!filterBastType(clazz.getName())) {
                    cyclicProperty.add(clazz);
                }
                apiParameters.addAll(createFormDataParameter(clazz, apiParameter, parentName + "[0]", prefix, cyclicProperty, propertyMap));
            }
        } else {
            try {
                if (((Class<?>) gType).isArray()) {
                    apiParameter.setUnderline(underline);
                    apiParameter.setName(apiParameter.getName() + "[0]");
                    getFormDataPropertyType(apiParameters, ((Class<?>) gType).getComponentType(), apiParameter, apiParameter.getName() + "[0]", prefix, cyclicProperty, propertyMap);
                } else {
                    Class<?> clazz = (Class<?>) gType;
                    //如果最终为基本数据类型则不再解析
                    if (filterBastType(clazz.getName())) {
                        apiParameters.add(apiParameter);
                        return;
                    }
                    if (cyclicProperty.contains(clazz)) {
                        //发现循环依赖则直接进入下一循环
                        return;
                    }
                    //加入到循环依赖Set中
                    if (!filterBastType(clazz.getName())) {
                        cyclicProperty.add(clazz);
                    }
                    apiParameters.addAll(createFormDataParameter(clazz, apiParameter, "", prefix, cyclicProperty, propertyMap));
                }
            } catch (ClassCastException e) {
                log.error("获取泛型类型出错！===>propertyName:{},typeName:{}", apiParameter.getName(), gType.getTypeName());
            }
        }
    }

    /**
     * 过滤基本类型
     */
    public boolean filterBastType(String type) {
        return basicTypes.contains(type);
    }


    /**
     * 创建出参Api
     */
    public void createMethodsOutParameter(Method method, ApiMethod apiMethod) {
        Class<?> returnType = method.getReturnType();
        ApiParameter apiParameter = new ApiParameter();
        apiParameter.setUnderline(underline);
        apiParameter.setName(returnType.getSimpleName());
        apiParameter.setRequire(true);
        apiParameter.setTypeFullName(returnType.getName());
        ApiModel apiModel = returnType.getAnnotation(ApiModel.class);
        if (apiModel != null) {
            String name = apiModel.name();
            if (StringUtils.isNotBlank(name)) {
                apiParameter.setUnderline(underline);
                apiParameter.setName(name);
            }
            String description = apiModel.description();
            apiParameter.setDescription(description);
        }
        Type genericReturnType = method.getGenericReturnType();
        //用于循环依赖判定
        Set<Class<?>> cyclicProperty = new HashSet<>();
        cyclicProperty.add(returnType);
        apiParameter.setApiParameters(createJsonParameter(returnType, genericReturnType, "get", cyclicProperty, new HashMap<>()));
        apiMethod.setOutApiParameter(apiParameter);
    }

    /**
     * 创建Json形式属性Api
     */
    public Set<ApiParameter> createJsonParameter(Class<?> clazz, Type genericReturnType, String prefix, Set<Class<?>> cyclicProperty, Map<String, Boolean> propertyMap) {
        Set<ApiParameter> apiParameters = new HashSet<>();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith(prefix)) {
                methodName = StringUtils.uncapitalize(methodName.replaceFirst(prefix, ""));
            }
            ApiParameter apiParameter = new ApiParameter();
            try {
                Field field = clazz.getDeclaredField(methodName);
                apiParameter.setUnderline(underline);
                apiParameter.setName(field.getName());
                apiParameter.setTypeFullName(field.getType().getName());
                ApiProperty apiProperty = field.getAnnotation(ApiProperty.class);
                if (apiProperty != null) {
                    if (apiProperty.hidden()) {
                        continue;
                    }
                    String name = apiProperty.name();
                    String example = apiProperty.example();
                    String description = apiProperty.description();
                    String type = apiProperty.type();
                    boolean required = apiProperty.required();
                    String defaultValue = apiProperty.defaultValue();
                    if (StringUtils.isNotBlank(name)) {
                        apiParameter.setUnderline(underline);
                        apiParameter.setName(name);
                    }
                    apiParameter.setDescription(description);
                    if (!StringUtils.equals(ValueConstants.DEFAULT_NONE, defaultValue)) {
                        apiParameter.setDefaultValue(defaultValue);
                    }
                    if (StringUtils.isNotBlank(type)) {
                        apiParameter.setType(type);
                    }
                    apiParameter.setRequire(required);
                    apiParameter.setExample(example);
                }
                //加入到循环依赖Set中
                if (!filterBastType(clazz.getName())) {
                    //先把当前类加进去
                    cyclicProperty.add(clazz);
                }
                if (!propertyMap.isEmpty()) {
                    if (!propertyMap.containsKey(apiParameter.getName())) {
                        //非指定的参数跳过
                        continue;
                    }
                    apiParameter.setRequire(propertyMap.get(apiParameter.getName()));
//                    propertyMap.remove(apiParameter.getName());
                }
                if (genericReturnType != null) {
                    TypeVariable<? extends Class<?>>[] typeParameters = clazz.getTypeParameters();
                    if (typeParameters != null && typeParameters.length > 0) {
                        String name = typeParameters[0].getName();
                        if (StringUtils.equals(field.getGenericType().getTypeName(), name)) {
                            if (genericReturnType instanceof ParameterizedType) {
                                // 强制类型转换
                                ParameterizedType pType = (ParameterizedType) genericReturnType;
                                // 取得泛型类型的泛型参数
                                Type[] tArgs = pType.getActualTypeArguments();
                                apiParameters.add(apiParameter);
                                try {
                                    //判断是否是数组
                                    if ((tArgs[0]) instanceof Class && ((Class<?>) tArgs[0]).isArray()) {
                                        apiParameter.setTypeFullName(((Class<?>) tArgs[0]).getCanonicalName());
                                        getJsonPropertyType(((Class<?>) tArgs[0]).getComponentType(), apiParameter, prefix, cyclicProperty, propertyMap);
                                        continue;
                                    }
                                    assert tArgs[0] instanceof ParameterizedType;
                                    //判断是否是集合
                                    if (Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) tArgs[0]).getRawType())) {
                                        apiParameter.setTypeFullName(pType.getRawType().getTypeName());
                                    }
                                } catch (ClassCastException e) {
                                    apiParameter.setTypeFullName(((Class<?>) tArgs[0]).getName());
                                    //处理基本类型参数
                                    if (filterBastType(((Class<?>) tArgs[0]).getName())) {
                                        continue;
                                    }
                                }
                                getJsonPropertyType(tArgs[0], apiParameter, prefix, cyclicProperty, propertyMap);
                                continue;
                            } else if ((genericReturnType instanceof Class) && ((Class<?>) genericReturnType).isArray()) {
                                if (((Class<?>) genericReturnType).isArray()) {
                                    apiParameter.setTypeFullName(((Class<?>) genericReturnType).getCanonicalName());
                                    getJsonPropertyType(((Class<?>) genericReturnType).getComponentType(), apiParameter, prefix, cyclicProperty, propertyMap);
                                    continue;
                                }
                            }
                        }
                    }
                }
                //处理集合
                if (Collection.class.isAssignableFrom(field.getType())) {
                    apiParameter.setTypeFullName(field.getType().getName());
                    getJsonPropertyType(field.getGenericType(), apiParameter, "set", cyclicProperty, propertyMap);
                } else if (field.getType().isArray()) {
                    apiParameter.setTypeFullName(field.getType().getCanonicalName());
                    getJsonPropertyType(field.getType().getComponentType(), apiParameter, prefix, cyclicProperty, propertyMap);
                } else {
                    //加入到循环依赖Set中
                    if (!filterBastType(field.getType().getName())) {
                        cyclicProperty.add(field.getType());
                    }
                    //如果最终为基本数据类型则不再解析
                    if (filterBastType(field.getType().getName())) {
                        apiParameters.add(apiParameter);
                        continue;
                    }
                    if (apiProperty == null) {
                        //json形式解析类模型注解
                        ApiModel apiModel = field.getType().getAnnotation(ApiModel.class);
                        if (apiModel != null) {
                            String name = apiModel.name();
                            if (StringUtils.isNotBlank(name)) {
                                apiParameter.setUnderline(underline);
                                apiParameter.setName(name);
                            }
                            String description = apiModel.description();
                            apiParameter.setDescription(description);
                        }
                    }
                    //发现循环依赖则不进行递归
                    if (!cyclicProperty.contains(field.getType())) {
                        //递归创建子参数
                        apiParameter.setApiParameters(createJsonParameter(field.getType(), genericReturnType, "set", cyclicProperty, propertyMap));
                    }
                }

            } catch (NoSuchFieldException e) {
                continue;
            }
            apiParameters.add(apiParameter);
        }
        return apiParameters;
    }

    /**
     * 处理集合泛型Json
     */
    public void getJsonPropertyType(Type gType, ApiParameter apiParameter, String prefix, Set<Class<?>> cyclicProperty, Map<String, Boolean> propertyMap) {
        // 如果gType类型是ParameterizedType对象
        if (gType instanceof ParameterizedType) {
            // 强制类型转换
            ParameterizedType pType = (ParameterizedType) gType;
            // 取得泛型类型的泛型参数
            Type[] tArgs = pType.getActualTypeArguments();
            if ((tArgs[0]) instanceof Class && ((Class<?>) (tArgs[0])).isArray()) {
                //处理数组
                ApiParameter childApiParameter = new ApiParameter();
                Set<ApiParameter> apiParameters = new HashSet<>();
                childApiParameter.setTypeFullName(((Class<?>) tArgs[0]).getCanonicalName());
                apiParameters.add(childApiParameter);
                apiParameter.setApiParameters(apiParameters);
                getJsonPropertyType(((Class<?>) tArgs[0]).getComponentType(), childApiParameter, prefix, cyclicProperty, propertyMap);
            } else if ((tArgs[0]) instanceof ParameterizedType && Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) tArgs[0]).getRawType())) {
                //处理集合
                ApiParameter childApiParameter = new ApiParameter();
                Set<ApiParameter> apiParameters = new HashSet<>();
                childApiParameter.setTypeFullName(pType.getRawType().getTypeName());
                apiParameters.add(childApiParameter);
                apiParameter.setApiParameters(apiParameters);
                getJsonPropertyType(tArgs[0], childApiParameter, prefix, cyclicProperty, propertyMap);
            } else {
                //处理类
                getJsonPropertyType(tArgs[0], apiParameter, prefix, cyclicProperty, propertyMap);
            }
        } else {
            ApiParameter childApiParameter = new ApiParameter();
            Set<ApiParameter> apiParameters = new HashSet<>();
            apiParameters.add(childApiParameter);
            apiParameter.setApiParameters(apiParameters);
            assert gType instanceof Class<?>;
            Class<?> clazz = (Class<?>) gType;
            if (cyclicProperty.contains(clazz)) {
                //发现循环依赖则直接终止
                return;
            }
            //处理数组
            if (clazz.isArray()) {
                childApiParameter.setTypeFullName(clazz.getCanonicalName());
                getJsonPropertyType(clazz.getComponentType(), childApiParameter, prefix, cyclicProperty, propertyMap);
                return;
            }
            //加入到循环依赖Set中
            if (!filterBastType(clazz.getName())) {
                cyclicProperty.add(clazz);
            }
            //json形式解析类模型注解
            childApiParameter.setTypeFullName(clazz.getName());
            //处理基本类型参数
            if (filterBastType(clazz.getName())) {
                return;
            }
            childApiParameter.setName(clazz.getSimpleName());
            if (StringUtils.isBlank(apiParameter.getName())) {
                ApiModel apiModel = clazz.getAnnotation(ApiModel.class);
                if (apiModel != null) {
                    String name = apiModel.name();
                    if (StringUtils.isNotBlank(name)) {
                        childApiParameter.setName(name);
                    }
                    String description = apiModel.description();
                    childApiParameter.setDescription(description);
                }
            }
            childApiParameter.setApiParameters(createJsonParameter(clazz, null, prefix, cyclicProperty, propertyMap));
        }
    }
}
