package com.example.service;

import com.spring.Autowired;
import com.spring.Component;
import com.spring.InitializingBean;
import com.spring.Scope;

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
