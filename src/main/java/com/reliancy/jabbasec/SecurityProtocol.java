package com.reliancy.jabbasec;

import java.io.IOException;

import com.reliancy.jabba.Request;
import com.reliancy.util.Handy;

/**
 * A SecurityProtocol will be processing HTTP to establish SecurityActor or user.
 */
public abstract class SecurityProtocol {
    protected final String name;
    public SecurityProtocol(String n){
        name=n;
    }
    public String getName(){
        return name;
    }
    public abstract SecurityActor authenticate(SecurityPolicy policy,Request req,String tok)  throws NotPermitted, IOException ;
    public static class Basic extends SecurityProtocol{
        public Basic(){
            super("Basic");
        }
        @Override
        public SecurityActor authenticate(SecurityPolicy policy,Request req,String tok) throws NotPermitted, IOException {
            tok=String.valueOf(Handy.decodeBase64(tok)) ;
            String[] up=tok.split(":",2);
            String userid=up[0];
            String pwd=up.length>1?up[1]:"";
            return policy.loadActor(policy.admin,userid,pwd);
        }
    }
    public static class Digest extends SecurityProtocol{
        public Digest(){
            super("Digest");
        }
        @Override
        public SecurityActor authenticate(SecurityPolicy policy,Request req,String tok) {
            return null;
        }
    }
}
