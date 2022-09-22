/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;
/**
 * An object describing what rights an actor has on a securable.
 * This object can be one individual rule or an effective merge of multiple rights.
 * In any case constructing this class will not be allowed except by security policy. 
 * We should start implementing from security policy and maybe we do not even need this class.
 */
public interface SecurityPermit {
    public SecurityStore getStore();
    public Integer getId();
    public SecurityActor getActor();
    public Securable getSubject();
    public boolean canRead();
    public boolean canWrite();
    public boolean canDelete();
    public boolean canCreate();
    public boolean canSecure();
    public boolean canExecute();
}
