package com.reliancy.jabbasec;

import java.util.List;

/**
 * Any entity that can be secured or permissions set for an actor.
 */
public interface Securable {
    public Integer getId();
    public Integer getOwnerId();
    public String getKind();
    public String getName();
    public String getTitle();
    public String getIcon();
    public Securable getOwner();
    public List<Securable> getOwnedSecurables();
    public List<SecurityPermit> getDirectPermits();
    public SecurityPermit getPermit(Securable sec);
    public SecurityPolicy getPolicy();
}
