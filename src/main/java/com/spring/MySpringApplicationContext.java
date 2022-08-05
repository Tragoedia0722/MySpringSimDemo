package com.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class MySpringApplicationContext {
    private final Class<?> configClass; // 配置类
    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>(); // Bean定义Map
    private final Map<String, Object> singletonObjects = new HashMap<>(); // 单例池
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>(); // 后置对象缓存列表

    /**
     * 构造函数，读取配置类
     */
    public MySpringApplicationContext(Class<?> configClass) {
        this.configClass = configClass;
        scan(configClass);

        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }
    }

    /**
     * 扫描注解获取文件目录
     * 加载类文件获取类注解
     * 将 Bean 信息添加到 BeanDefinitionMap 中
     */
    public void scan(Class<?> configClass) {
        // 扫描注解获取文件路径
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScan = configClass.getAnnotation(ComponentScan.class);
            String scanPath = componentScan.value();
            scanPath = scanPath.replace(".", "/");
            ClassLoader classLoader = configClass.getClassLoader();
            URL resource = classLoader.getResource(scanPath);
            if (Objects.nonNull(resource)) {
                File file = new File(resource.getFile());
                if (file.isDirectory()) {
                    // 遍历目录下文件
                    for (File f : Objects.requireNonNull(file.listFiles())) {
                        String absolutePath = f.getAbsolutePath();
                        absolutePath = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class")).replace("\\", ".");
                        // 加载类文件获取注解
                        try {
                            Class<?> clazz = classLoader.loadClass(absolutePath);
                            if (clazz.isAnnotationPresent(Component.class)) {
                                // BeanPostProcessor实现
                                if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                    BeanPostProcessor beanPostProcessor = (BeanPostProcessor) clazz.getConstructor().newInstance();
                                    beanPostProcessors.add(beanPostProcessor);
                                }
                                // Bean信息添加到BeanDefinitionMap中
                                else {
                                    BeanDefinition beanDefinition = new BeanDefinition();
                                    Component component = clazz.getAnnotation(Component.class);
                                    String beanName = component.value();
                                    if ("".equals(beanName)) {
                                        beanName = Introspector.decapitalize(clazz.getSimpleName());
                                    }
                                    beanDefinition.setType(clazz);
                                    if (clazz.isAnnotationPresent(Scope.class)) {
                                        Scope scope = clazz.getAnnotation(Scope.class);
                                        String value = scope.value();
                                        beanDefinition.setScope(value);
                                    } else {
                                        beanDefinition.setScope("singleton");
                                    }
                                    beanDefinitionMap.put(beanName, beanDefinition);
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 根据 BeanDefinition 创建 Bean 对象
     */
    public Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class<?> clazz = beanDefinition.getType();
        Object instance;
        try {
            instance = clazz.getConstructor().newInstance();
            // 遍历属性，查看是否有依赖注入注解 Autowired
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    field.set(instance, getBean(field.getName()));
                }
            }
            // 初始化前方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                instance = beanPostProcessor.postProcessorBeforeInitializing(instance, beanName);
            }
            // 初始化方法
            if (instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }
            // 初始化后方法
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                instance = beanPostProcessor.postProcessorAfterInitializing(instance, beanName);
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return instance;
    }

    /**
     * 根据 BeanDefinition 获取 Bean 对象
     */
    public Object getBean(String beanName) {
        if (!beanDefinitionMap.containsKey(beanName)) {
            throw new RuntimeException("Bean不存在");
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        String scope = beanDefinition.getScope();
        if (scope.equals("singleton")) {
            Object singletonBean = singletonObjects.get(beanName);
            // 解决依赖与单例Bean创建顺序不一致导致的从单例池返回结果为null的问题
            if (singletonBean == null) {
                singletonBean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, singletonBean);
            }
            return singletonBean;
        }
        if (scope.equals("prototype")) {
            return createBean(beanName, beanDefinition);
        }
        return null;
    }
}
