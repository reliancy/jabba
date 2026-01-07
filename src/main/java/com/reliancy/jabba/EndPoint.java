/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/** EndPoint is a special processor usually the last in chain.
 * 
 */
public abstract class EndPoint extends Processor{

    public EndPoint(String id) {
        super(id);
    }
    public abstract void serve(Request request, Response response) throws IOException;    
}
