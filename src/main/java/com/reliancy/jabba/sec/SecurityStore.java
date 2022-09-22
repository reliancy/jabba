/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;

import java.io.IOException;
import java.util.List;

/**
 * Storage interface for security elemenets such as securables, actors and permits.
 * Storage is unlocked until a policy is set then it gets locked and it matters who
 * admin actor is.
 * 
 * The lock is establish witht the install of a policy. The outside code including policy can 
 * query any securable and especially admin account. Once the policy is set then admin account
 * must be used for privileged access.
 */
public interface SecurityStore {
    public static int GUEST=-1;
    public static int ADMIN=-2;

    public Securable newSecurable();
    public SecurityActor newActor();
    public SecurityPermit newPermit();
    public void deleteSecurable(SecurityActor actor, Securable sec) throws IOException;
    public void saveSecurable(SecurityActor actor, Securable sec) throws IOException;
    public Securable loadSecurable(SecurityActor actor, Integer id) throws IOException, NotPermitted;
    public SecurityActor loadActor(SecurityActor actor, String name, String pwd) throws IOException, NotPermitted;
    public List<Securable> loadSecurables(SecurityActor actor, Securable sec) throws IOException, NotPermitted;
    public void deletePermit(SecurityActor actor, SecurityPermit permit) throws IOException;
    public void savePermit(SecurityActor actor, SecurityPermit permit) throws IOException;
    public SecurityPermit loadPermit(SecurityActor actor, Integer id) throws IOException, NotPermitted;
    public List<SecurityPermit> loadPermitsBy(SecurityActor actor, SecurityActor sec) throws IOException, NotPermitted;
    public List<SecurityPermit> loadPermitsOn(SecurityActor actor, Securable sec) throws IOException, NotPermitted;
    public void setPolicy(SecurityPolicy policy);
    public SecurityPolicy getPolicy();
}
