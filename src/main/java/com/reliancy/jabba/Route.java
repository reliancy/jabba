package com.reliancy.jabba;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Route {
    String path() default "{method}";
    String verb() default "GET|POST|DELETE";
    String return_mime() default "";
}
