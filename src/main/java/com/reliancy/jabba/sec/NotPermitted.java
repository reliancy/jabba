/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;
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
