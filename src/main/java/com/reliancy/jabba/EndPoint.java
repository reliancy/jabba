/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.IOException;

public abstract class EndPoint extends Processor{

    public EndPoint(String id) {
        super(id);
    }
    @Override
    public void before(Request request, Response response) throws IOException {
    }
    @Override
    public void after(Request request, Response response) throws IOException {
    }
    public abstract void serve(Request request, Response response) throws IOException;
    
}
