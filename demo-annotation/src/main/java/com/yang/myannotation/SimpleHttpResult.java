package com.yang.myannotation;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SimpleHttpResult implements HttpResult<String> {
    private String result;
    private int status;

    @Override
    public String getResponse() {
        return this.result;
    }

    @Override
    public int getStatus() {
        return this.status;
    }
}
