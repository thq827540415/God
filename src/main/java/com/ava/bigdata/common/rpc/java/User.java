package com.ava.bigdata.common.rpc.java;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class User implements Serializable {
    private String name;
    private int age;

    public User() {
    }

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
