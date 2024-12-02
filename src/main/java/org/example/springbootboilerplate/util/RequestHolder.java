package org.example.springbootboilerplate.util;

import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
public class RequestHolder {
    private final static ThreadLocal<Map<String, String>> requestHeaders = new ThreadLocal<>();

    public static Map<String, String> getRequestHeaders() {
        return requestHeaders.get();
    }

    public static void setRequestHeaders(Map<String, String> requestHeaders) {
        RequestHolder.requestHeaders.set(requestHeaders);
    }

    public static void clearRequestHeaders() {
        requestHeaders.remove();
    }
}
