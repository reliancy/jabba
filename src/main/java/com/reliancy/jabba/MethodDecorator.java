/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

/** Decorator is a handler for method annotations.
 * The name is borrowed from python because it connects java annotations to handler that are called before and after a method is invoked.
 * Decorator itself is injected and called by methodendpoint as a filter but the factory does not have to return an object.
 * it can just use the methodendpoint to register it somewhere once during startup.
 */
public abstract class MethodDecorator {
    public static interface Factory{
        MethodDecorator  assertDecorator(MethodEndPoint mep,Annotation ann);
    }
    static final ArrayList<MethodDecorator.Factory> registry=new ArrayList<>();
    public static void publish(MethodDecorator.Factory d){
        if(!registry.contains(d)) registry.add(d);
    }
    public static void retract(MethodDecorator.Factory d){
        while (registry.remove(d));
    }
    public static MethodDecorator query(MethodEndPoint mep,Annotation ann){
        for(MethodDecorator.Factory f:registry){
            MethodDecorator d=f.assertDecorator(mep, ann);
            if(d!=null) return d;
        }
        return null;
    }
    MethodEndPoint method;
    Annotation annotation;
    public MethodDecorator(MethodEndPoint mep,Annotation ann){
        method=mep;
        annotation=ann;
    }
    public abstract void beforeMethod(Request request, Response response);
    public abstract void afterMethod(Request request, Response response);
}
