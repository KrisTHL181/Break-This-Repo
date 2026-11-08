package com.hodastar.photosreview.utils;

public class Respond<T> {
    public boolean result;
    public String message;
    public T data;

    public Respond() {}

    public Respond(boolean result, String message, T data) {
        this.result = result;
        this.message = message;
        this.data = data;
    }
}
