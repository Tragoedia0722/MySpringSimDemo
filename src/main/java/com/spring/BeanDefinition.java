package com.spring;

/**
 * 定义Bean
 */
public class BeanDefinition {
    private Class<?> type;
    private String scope;
    private boolean bLazy;

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isbLazy() {
        return bLazy;
    }

    public void setbLazy(boolean bLazy) {
        this.bLazy = bLazy;
    }
}
