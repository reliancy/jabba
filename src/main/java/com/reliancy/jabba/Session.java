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
