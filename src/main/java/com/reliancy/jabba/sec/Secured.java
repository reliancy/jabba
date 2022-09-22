/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/** Annotation to indicate that this resource requruies authenticated actor and/or permit.
 * first of all we register this route with securitypolicy and enforce actor presence.
 * if user is not logged in we send to login form or use one of our protocols.
 * Additionally we can specify permits that are required.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Secured {
    String login_form() default "";
    String permits() default "";
}
