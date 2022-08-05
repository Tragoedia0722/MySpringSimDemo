package com.example.service;

import com.spring.Component;
import com.spring.Scope;

@Component(value = "userService")
//@Scope(value = "prototype")
@Scope(value = "singleton")
public class UserService {
    public void test() {
        System.out.println("userService test!");
    }
}
