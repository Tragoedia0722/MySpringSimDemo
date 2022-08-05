package com.spring;

/**
 * 初始化方法接口，实现该接口方法进行接口的初始化
 */
public interface InitializingBean {
    void afterPropertiesSet();
}
