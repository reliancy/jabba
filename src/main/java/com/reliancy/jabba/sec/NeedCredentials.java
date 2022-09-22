/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.sec;

import com.reliancy.util.CodeException;
import com.reliancy.util.ResultCode;

public class NeedCredentials extends CodeException {
    public static final int CODE=ResultCode.defineFailure(1,SecurityPolicy.class,"please provide credentials");
    
    public NeedCredentials(){
        super(CODE);
    }
    
}
