/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;

/**
 * Interface that is implemented by any User or Principal entity.
 * Often an AppSession will be determined in many ways by the user. 
 * SecurityActor is a very special type of Securable.
 */
public interface SecurityActor extends Securable{
    SecurityActor authPassword(String password);
    /** returns HA1 signature for Digest protocol. */
    String getDigestSignature(String realm);
    default boolean isRole(){
        return false;
    };
}
