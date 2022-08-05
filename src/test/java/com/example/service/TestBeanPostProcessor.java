package com.example.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

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
