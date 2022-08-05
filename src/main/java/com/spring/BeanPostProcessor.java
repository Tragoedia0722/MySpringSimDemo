package com.spring;

/**
 * Bean后置处理器
 */
public interface BeanPostProcessor {
    default Object postProcessorBeforeInitializing(Object bean, String beanName) {
        return bean;
    }

    default Object postProcessorAfterInitializing(Object bean, String beanName) {
        return bean;
    }
}
