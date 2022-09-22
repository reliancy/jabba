/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;

import java.util.List;

import com.reliancy.util.Handy;

/**
 * Any entity that can be secured or permissions set for an actor.
 */
public interface Securable {
    public SecurityStore getStore();
    public Integer getId();
    default public Integer getOwnerId(){
        return getOwner().getId();
    }
    public String getKind();
    public String getName();
    default public String getTitle(){
        return Handy.prettyPrint(getName());        
    }
    public String getIcon();
    public Securable getOwner();
    public List<Securable> getOwnedSecurables();
    public List<SecurityPermit> getDirectPermits();
    public SecurityPermit getPermit(Securable sec);
    public SecurityPolicy getPolicy();
    public default boolean isEssential(){
        return false;
    };
}
