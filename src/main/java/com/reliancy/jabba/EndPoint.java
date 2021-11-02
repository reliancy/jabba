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
