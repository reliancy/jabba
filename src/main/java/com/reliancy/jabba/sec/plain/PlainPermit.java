/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec.plain;

import com.reliancy.jabba.sec.Securable;
import com.reliancy.jabba.sec.SecurityActor;
import com.reliancy.jabba.sec.SecurityPermit;
import com.reliancy.jabba.sec.SecurityStore;

public class PlainPermit implements SecurityPermit{
    SecurityStore store;
    Integer id;
    SecurityActor actor;
    Securable subject;
    boolean can_read;
    boolean can_write;
    boolean can_delete;
    boolean can_create;
    boolean can_secure;
    boolean can_execute;

    @Override
    public SecurityStore getStore(){
        return store;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public SecurityActor getActor() {
        return actor;
    }

    @Override
    public Securable getSubject() {
        return subject;
    }

    @Override
    public boolean canRead() {
        return can_read;
    }

    @Override
    public boolean canWrite() {
        return can_write;
    }

    @Override
    public boolean canDelete() {
        return can_delete;
    }

    @Override
    public boolean canCreate() {
        return can_create;
    }

    @Override
    public boolean canSecure() {
        return can_secure;
    }
    @Override
    public boolean canExecute() {
        return can_execute;
    }
    
}
