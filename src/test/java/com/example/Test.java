package com.example;

import com.example.service.TestService;
import com.spring.MySpringApplicationContext;

public class Test {
    public static void main(String[] args) {
        MySpringApplicationContext applicationContext = new MySpringApplicationContext(AppConfig.class);
        TestService testService = (TestService) applicationContext.getBean("testService");
        testService.test();
        testService.wiredTest();
    }
}
