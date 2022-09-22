/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    /// returns the signature to be send to client.
    public String getSignature(String realm){
        return getName()+String.format(" realm=\"%s\"",realm);
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
        public SecurityActor authenticate(SecurityPolicy policy,Request req,String tok) throws NotPermitted, IOException{
            Map<String,String> params=decode_params(tok);
            String username=params.get("username");
            String realm=params.get("realm");
            String nonce=params.get("nonce");
            //String uri=params.get("uri");
            String rsp_in=params.get("response");
            SecurityActor usr=policy.loadActor(policy.admin,username,null);
            //#print("USER:",usr)
            if(usr!=null){
                String ha1=usr.getDigestSignature(realm);
                String rsp_usr=digest_signature(ha1,nonce,req.getVerb(),req.getPath());
                //print("CHECK:",rsp_usr,rsp_in,uri,req.full_path)
                if(!rsp_usr.equals(rsp_in)){
                    //this user response does not match one provided
                    usr=null;
                }
            }
            if(usr==null) throw new NotAuthentic("Invalid credentials");
            return usr;
        }
        public String digest_signature(String ha1,String nonce,String method,String uri){
            //ha1_msg=user+":"+realm+":"+password
            //ha1=self.get_md5(ha1_msg)
            String ha2_msg=method+":"+uri;
            String ha2=Handy.hashMD5(ha2_msg);
            String rsp_msg=ha1+":"+nonce+":"+ha2;
            String rsp=Handy.hashMD5(rsp_msg);
            return rsp;
        }
        /** return a dict of all the values sent.*/ 
        public Map<String,String> decode_params(String tok){
            HashMap<String,String> ret=new HashMap<>();
            for(String kv:tok.trim().split(",")){
                String[] args=kv.trim().split("=",2);
                String k=Handy.trimRight(args[0]," \t\r\f\n");
                String v=args.length>1?Handy.trimEvenly(args[1],"'\""):"";
                ret.put(k,v);
            }
            return ret;
        }
        /// returns a new nonce value.
        public String new_nonce(){
            //String nonce=util.get_md5(str(id(self)))
            String nonce=Handy.hashMD5(String.valueOf(hashCode()));
            return nonce;
        }
        @Override
        public String getSignature(String realm){
            //return getName()+String.format(" realm=\"%s\"",realm);
            String nonce=new_nonce();
            String ret=super.getSignature(realm);
            return String.format("%s, nonce=\"%s\"",ret,nonce);
        }
    
    }
}
