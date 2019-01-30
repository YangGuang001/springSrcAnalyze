package com.yang.myannotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableHttpUtil(basePackage = "com.yang.myannotation")
public class HttpReuestDemo {
    public static void main(String[] args) {
        SpringApplication.run(HttpReuestDemo.class, args);
    }
}
