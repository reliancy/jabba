/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

/** Session is temporary storage. 
 * we will have application session but also call session.
 * for methodendpoints we must still be able to access request,response objects.
 * 
*/
public interface Session {
    public void setValue(String key,Object val);
    public Object getValue(String key);
}
