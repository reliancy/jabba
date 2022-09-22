/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec.plain;

import com.reliancy.jabba.sec.SecurityActor;
import com.reliancy.util.Handy;

public class PlainActor extends PlainSecurable implements SecurityActor{
    String password;
    boolean role;

    @Override
    public SecurityActor authPassword(String pwd) {
        if(password.equals(pwd)) return this;
        return null;
    }
    
    protected String getPassword() {
        return password;
    }

    protected PlainActor setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getDigestSignature(String realm){
        String ha1_msg=this.getName()+":"+realm+":"+password;
        String ha1=Handy.hashMD5(ha1_msg);
        return ha1;
    }

    public boolean isRole() {
        return role;
    }

    public void setRole(boolean role) {
        this.role = role;
    }
        
}
