package com.yang.myannotation;

public interface HttpResult<T> {
    T getResponse();

    int getStatus();
}
