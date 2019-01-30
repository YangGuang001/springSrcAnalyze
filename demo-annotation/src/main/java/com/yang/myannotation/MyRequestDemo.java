package com.yang.myannotation;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Component
@HTTPUtil
public interface MyRequestDemo {
    @HTTPRequest(url = "http://abc.com")
    HttpResult<String> test1();

    @HTTPRequest(url = "http://test2.com", httpMethod = HTTPMethod.POST)
    HttpResult<String> test2();
}
