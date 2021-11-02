package com.reliancy.jabbasec;

public class NotAuthentic extends RuntimeException {
    public NotAuthentic(String message){
        super(message);
    }
    public NotAuthentic(String message,Throwable cause){
        super(message,cause);
    }
    
}
