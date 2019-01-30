package com.yang.myannotation;

import java.lang.reflect.Method;

public interface HTTPHandler {
    HttpResult<?> handle(Method method);
}
