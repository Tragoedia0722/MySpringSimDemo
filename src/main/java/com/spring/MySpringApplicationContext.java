package com.spring;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MySpringApplicationContext {
    private final Class<?> configClass; // 配置类
    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>(); // Bean定义Map
    private final Map<String, Object> singletonObjects = new HashMap<>(); // 单例池

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
    private void scan(Class<?> configClass) {
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
                                // Bean信息添加到BeanDefinitionMap中
                                BeanDefinition beanDefinition = new BeanDefinition();
                                Component component = clazz.getAnnotation(Component.class);
                                String beanName = component.value();
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
                        } catch (ClassNotFoundException e) {
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
        Class<?> definitionType = beanDefinition.getType();
        Object bean;
        try {
            bean = definitionType.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bean;
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
            return singletonObjects.get(beanName);
        }
        if (scope.equals("prototype")) {
            return createBean(beanName, beanDefinition);
        }

        return null;
    }
}
