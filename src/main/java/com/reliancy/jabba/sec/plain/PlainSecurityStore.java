/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec.plain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.reliancy.dbo.Bag;
import com.reliancy.jabba.Path;
import com.reliancy.jabba.sec.NotPermitted;
import com.reliancy.jabba.sec.Securable;
import com.reliancy.jabba.sec.SecurityActor;
import com.reliancy.jabba.sec.SecurityPermit;
import com.reliancy.jabba.sec.SecurityPolicy;
import com.reliancy.jabba.sec.SecurityStore;


/**
 * PlainSecurityStore is a container for plain security elements.
 * It will implement a simple in-memory list of items. Optionally it will
 * have be able to load or save its state to disk as json.
 * 
 *
 */
public class PlainSecurityStore implements SecurityStore {
    final Bag<PlainSecurable> securables=new Bag<PlainSecurable>();
    final Bag<PlainPermit> permits=new Bag<PlainPermit>();
    SecurityPolicy policy;
    PlainActor guest;
    PlainActor admin;
    Path path;
    
    public PlainSecurityStore() {
        guest=(PlainActor) newActor();
        guest.setPassword("").setName("guest").setEssential(true);
        admin=(PlainActor) newActor();
        admin.setPassword("admin").setName("admin").setTitle("Administrator").setEssential(true);
        securables.add(guest);
        securables.add(admin);
    }
    public PlainSecurityStore setPath(Path p){
        path=p;
        return this;
    }
    public void load() throws IOException{

    }
    public void save() throws IOException{

    }
    @Override
    public Securable newSecurable() {
        return new PlainSecurable();
    }

    @Override
    public SecurityActor newActor() {
        return new PlainActor();
    }

    @Override
    public SecurityPermit newPermit() {
        return new PlainPermit();
    }

    @Override
    public void deleteSecurable(SecurityActor actor, Securable sec) throws IOException {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin && actor!=sec.getOwner()) throw new NotPermitted("admin or owner rights required");
        }
        securables.remove((PlainSecurable)sec);
    }

    @Override
    public void saveSecurable(SecurityActor actor, Securable sec) throws IOException {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin && actor!=sec.getOwner()) throw new NotPermitted("admin or owner rights required");
        }
        securables.add((PlainSecurable)sec);
    }

    @Override
    public Securable loadSecurable(SecurityActor actor, Integer id) throws IOException, NotPermitted {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin) throw new NotPermitted("admin rights required");
        }
        if(id==ADMIN) return admin;
        if(id==GUEST) return guest;
        for(Securable sec:securables){
            if(sec.getId()==id) return sec;
        }
        return null;
    }

    @Override
    public SecurityActor loadActor(SecurityActor actor, String name, String pwd) throws IOException, NotPermitted {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin) throw new NotPermitted("admin rights required");
        }
        for(Securable sec:securables){
            if(!(sec instanceof SecurityActor)) continue; // skip over non actors
            SecurityActor a=(SecurityActor) sec;
            if(!a.getName().equalsIgnoreCase(name)) continue;       // name mismatch
            if(pwd!=null && a.authPassword(pwd)==a)  return a;      // match on password if provided
            boolean actor_permitted=actor==admin;
            if(!actor_permitted) continue;                          // actor is not permitted
            return a;                                               // if permitted lookup by name role or user
        }
        return null;
    }

    @Override
    public List<Securable> loadSecurables(SecurityActor actor, Securable sec) throws IOException, NotPermitted {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin && actor!=sec.getOwner()) throw new NotPermitted("admin or owner rights required");
        }
        ArrayList<Securable> ret=new ArrayList<>();
        for(Securable s:securables){
            if(sec==s.getOwner()) ret.add(s);
        }
        return ret;
    }

    @Override
    public void deletePermit(SecurityActor actor, SecurityPermit permit) throws IOException {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin) throw new NotPermitted("admin or owner rights required");
        }
        permits.remove((PlainPermit)permit);
    }

    @Override
    public void savePermit(SecurityActor actor, SecurityPermit permit) throws IOException {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin) throw new NotPermitted("admin or owner rights required");
        }
        permits.add((PlainPermit)permit);
    }

    @Override
    public SecurityPermit loadPermit(SecurityActor actor, Integer id) throws IOException, NotPermitted {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin) throw new NotPermitted("admin rights required");
        }
        for(SecurityPermit p:permits){
            if(p.getId()==id) return p;
        }
        return null;
    }
    @Override
    public List<SecurityPermit> loadPermitsBy(SecurityActor actor, SecurityActor sec) throws IOException, NotPermitted {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin) throw new NotPermitted("admin rights required");
        }
        ArrayList<SecurityPermit> ret=new ArrayList<>();
        for(SecurityPermit p:permits){
            if(p.getActor()==sec) ret.add(p);
        }
        return ret;
    }
    @Override
    public List<SecurityPermit> loadPermitsOn(SecurityActor actor, Securable sec) throws IOException, NotPermitted {
        if(policy!=null){
            // if policy is set only admin level users can access actors via securable
            if(actor!=admin) throw new NotPermitted("admin rights required");
        }
        ArrayList<SecurityPermit> ret=new ArrayList<>();
        for(SecurityPermit p:permits){
            if(p.getSubject()==sec) ret.add(p);
        }
        return ret;
    }

    @Override
    public void setPolicy(SecurityPolicy policy) {
        if(this.policy!=null) throw new IllegalStateException("Store is locked already.");
        this.policy=policy;
    }
    @Override
    public SecurityPolicy getPolicy() {
        return this.policy;
    }   
}
