package com.example;

import com.example.service.TestService;
import com.spring.MySpringApplicationContext;

public class Test {
    public static void main(String[] args) {
        // 扫描、创建单例Bean对象
        MySpringApplicationContext applicationContext = new MySpringApplicationContext(AppConfig.class);
        TestService testService = (TestService) applicationContext.getBean("testService");
        testService.test();
        testService.wiredTest();
    }
}
