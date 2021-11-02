package com.reliancy.jabbasec;
/**
 * Our own exception to throw when we are not allowed access.
 */
public class NotPermitted extends RuntimeException{
    public NotPermitted(String message){
        super(message);
    }
    public NotPermitted(String message,Throwable cause){
        super(message,cause);
    }
}
