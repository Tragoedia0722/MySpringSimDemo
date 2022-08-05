# Spring模拟实现

手动模拟Spring，实现了单例、原型Bean的创建，简单依赖注入，根据名称获取Bean实例对象，初始化与后置处理器等基本功能

## 注解

### ComponentScan

组件扫描注解，在配置类上添加，用于扫描指定包名下的组件

~~~java

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentScan {
    String value() default "";
}
~~~

### Component

组件注解，在需要被扫描的类上添加，用于指定该Bean的名称

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    String value() default "";
}
```

### Scope

作用域注解，用于指定该Bean对象是单例或原型

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scope {
    String value() default "";
}
```

### Autowired

自动注入注解，可添加至字段上

~~~JAVA
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Autowired {
	String value() default "";
}
~~~

## 接口

### InitializingBean

初始化方法接口，实现该接口方法进行初始化

```java
public interface InitializingBean {
    void afterPropertiesSet();
}
```

### BeanPostProcessor

Bean后置处理器，实现初始化前后的操作

~~~java
public interface BeanPostProcessor {
    default Object postProcessorBeforeInitializing(Object bean, String beanName) {
        return bean;
    }

    default Object postProcessorAfterInitializing(Object bean, String beanName) {
        return bean;
    }
}
~~~

## 容器类

### BeanDefinition

该类为Bean的定义对象，存放Bean的相关信息

~~~java
public class BeanDefinition {
    private Class<?> type; // Bean类型
    private String scope;  // Bean作用域
    private boolean bLazy; // 是否懒加载
	// getter & setter ...
}
~~~

### MySpringApplicaitonContext

该类为Spring容器，用于创建和管理Bean对象

~~~java
public class MySpringApplicationContext {
    // ...
}
~~~

#### 创建Bean对象

添加构造方法`MySpringApplicationContext`，用于读取配置类

~~~java
private final Class<?> configClass; // 配置类

public MySpringApplicationContext(Class<?> configClass) {
    this.configClass = configClass;
}
~~~

添加扫描方法`scan`，通过扫描配置类上`@ComponentScan`注解内的值来获取查找需要创建Bean对象的类，并将该Bean信息添加到BeanDefinitionMap中

~~~java
private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>(); // Bean定义Map

public MySpringApplicationContext(Class<?> configClass) {
    // ...
    scan(configClass);
}

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
                            // Bean信息添加到BeanDefinitionMap中
                            BeanDefinition beanDefinition = new BeanDefinition();
                            Component component = clazz.getAnnotation(Component.class);
                            String beanName = component.value();
                            if ("".equals(beanName)){
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
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
~~~

添加创建方法`createBean`，该方法通过Bean的定义类`BeanDefinition`来创建相应的Bean对象

修改构造方法`MySpringApplicationContext`，在构造时直接创建单例Bean对象，并将该对象存入单例池`singletonObjects`中，用于后续单例对象的调用

~~~java
private final Map<String, Object> singletonObjects = new HashMap<>(); // 单例池

public Object createBean(String beanName, BeanDefinition beanDefinition) {
    Class<?> clazz = beanDefinition.getType();
    Object instance;
    try {
        instance = clazz.getConstructor().newInstance();
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    return bean;
}

public MySpringApplicationContext(Class<?> configClass) {
    // ...
    for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
        String beanName = entry.getKey();
        BeanDefinition beanDefinition = entry.getValue();
        if (beanDefinition.getScope().equals("singleton")) {
            Object bean = createBean(beanName, beanDefinition);
            singletonObjects.put(beanName, bean);
        }
    }
}
~~~

添加`getBean`方法获取Bean对象，若为单例对象则从线程池中直接获取，若为原型对象则通过返回一个新的Bean对象，此时可以对单例与原型Bean进行基本的创建使用功能

~~~java
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
~~~

#### 依赖注入

修改`createBean`方法，在创建Bean对象后通过反射进行依赖注入为属性赋值

~~~java
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
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    return bean;
}
~~~

#### 初始化

修改`createBean`方法，实现初始化功能，实现`InitializingBean`接口并调用接口中的`afterPropertiesSet`方法

~~~java
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
        // 初始化方法
        if (instance instanceof InitializingBean) {
            ((InitializingBean) instance).afterPropertiesSet();
        }
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    return instance;
}
~~~

#### BeanPostProcessor

修改`scan`方法，在类获取到`@Component`注解后查询该类是否实现了`BeanPostProcessor`接口，若实现该接口则将该类添加到`beanPostProcessors`
缓存中，否则作为普通Bean添加信息到`BeanDefinitionMap`中

```java
private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>(); // 后置对象缓存列表

public void scan(Class<?> configClass) {
    // ...
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
    // ...
} 
```

修改`createBean`方法，在创建Bean初始化方法前后调用`BeanPostProcessor`的初始化前后方法`postProcessorBeforeInitializing`
和`postProcessorAfterInitializing`，通过该`BeanPostProcessor`可完成包括AOP、Aware在内的多种扩展方法

~~~java
public Object createBean(String beanName, BeanDefinition beanDefinition) {
	// ...
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
    // ...
}
~~~

## 测试类

### Appconfig

容器配置类，用于指定扫描的包

```java
@ComponentScan(value = "com.example.service")
public class AppConfig {
}
```

### Service

#### TestService

组件方法类，在类上添加`Component`注解指定该类为一个Bean容器

```java
@Component
@Scope(value = "prototype")
public class TestService implements InitializingBean {
    @Autowired
    private TestWiredService testWiredService;

    public void test() {
        System.out.println("testService test!");
    }

    public void wiredTest() {
        testWiredService.wiredTest();
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("testService 初始化");
    }
}
```

#### TestWiredService

测试注入类

~~~java
@Component
public class TestWiredService {
    public void wiredTest() {
        System.out.println("testWiredService test!");
    }
}
~~~

#### TestBeanPostProcessor

测试后置处理器

~~~java
@Component
public class TestBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessorBeforeInitializing(Object bean, String beanName) {
        System.out.println(beanName + " 初始化前方法");
        return BeanPostProcessor.super.postProcessorBeforeInitializing(bean, beanName);
    }

    @Override
    public Object postProcessorAfterInitializing(Object bean, String beanName) {
        System.out.println(beanName + " 初始化后方法");
        return BeanPostProcessor.super.postProcessorAfterInitializing(bean, beanName);
    }
}
~~~

### Main

主测试类，用于调用Spring容器，获取容器中的Bean对象并调用方法

```java
public class Test {
    public static void main(String[] args) {
        MySpringApplicationContext applicationContext = new MySpringApplicationContext(AppConfig.class);
        TestService testService = (TestService) applicationContext.getBean("testService");
        testService.test();
        testService.wiredTest();
    }
}
```

### Console

测试结果打印

~~~java
testWiredService 初始化前方法
testWiredService 初始化后方法
testService 初始化前方法
testService 初始化
testService 初始化后方法
testService test!
testWiredService test!
~~~