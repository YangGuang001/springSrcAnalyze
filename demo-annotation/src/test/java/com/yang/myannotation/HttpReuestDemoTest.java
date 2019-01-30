package com.yang.myannotation;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class HttpReuestDemoTest {

    @Autowired
    MyRequestDemo myRequestDemo;

    @Test
    public void test1() {
        HttpResult<String> result = myRequestDemo.test1();
        String response = result.getResponse();
        log.info(">>>>>>>>>>{}", response);
        assertEquals("http request: url=http://abc.com and method=GET",response);
    }

}
