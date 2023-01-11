/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec.plain;

import java.util.List;

import com.reliancy.jabba.sec.Securable;
import com.reliancy.jabba.sec.SecurityPermit;
import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.jabba.sec.SecurityStore;
import com.reliancy.util.Handy;

public class PlainSecurable implements Securable{
    SecurityStore store;
    Integer id;
    Securable owner;
    String kind;
    String name;
    String title;
    String icon;
    boolean essential;
    
    @Override
    public SecurityStore getStore() {
        return store;
    }
    @Override
    public Integer getId() {
        return id;
    }
    protected PlainSecurable setId(Integer id){
        this.id=id;
        return this;
    }
    @Override
    public String getKind() {
        return kind;
    }
    public PlainSecurable setKind(String v){
        kind=v;
        return this;
    }
    @Override
    public String getName() {
        return name;
    }
    public PlainSecurable setName(String v){
        name=v;
        return this;
    }

    @Override
    public String getTitle() {
        return title!=null?title:Handy.prettyPrint(getName());
    }
    public PlainSecurable setTitle(String v){
        title=v;
        return this;
    }

    @Override
    public String getIcon() {
        return icon;
    }
    public PlainSecurable setIcon(String v){
        icon=v;
        return this;
    }

    @Override
    public Securable getOwner() {
        return owner;
    }

    @Override
    public List<Securable> getOwnedSecurables() {
        return null;
    }

    @Override
    public List<SecurityPermit> getDirectPermits() {
        return null;
    }

    @Override
    public SecurityPermit getPermit(Securable sec) {
        return null;
    }

    @Override
    public SecurityPolicy getPolicy() {
        return store!=null?store.getPolicy():null;
    }
    public boolean isEssential() {
        return essential;
    }
    public PlainSecurable setEssential(boolean essential) {
        this.essential = essential;
        return this;
    }
    
}
