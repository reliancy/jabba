package com.reliancy.jabbasec;
/**
 * An object describing what rights an actor has on a securable.
 * This object can be one individual rule or an effective merge of multiple rights.
 * In any case constructing this class will not be allowed except by security policy. 
 * We should start implementing from security policy and maybe we do not even need this class.
 */
public interface SecurityPermit {
    public SecurityActor getActor();
    public Securable getSubject();
    public boolean canRead();
    public boolean canWrite();
    public boolean canDelete();
    public boolean canCreate();
    public boolean canSecure();
}
