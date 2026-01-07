package com.reliancy.jabba;

import java.io.IOException;

import com.reliancy.jabba.decor.Routed;

public class StatusMod implements AppModule{
    @Override
    public void publish(App app) {
        app.publishModule(this,getClass().getSimpleName());
        app.getRouter().importMethods(this);
    }
    @Routed(
        path="/status"
    )
    public void status(Request req,Response resp) throws IOException{
        StringBuilder buf=new StringBuilder();
        if(buf!=null) throw new IOException("bummer!!!");
        buf.append("Hi there!!!\n");
        buf.append("mount:").append(req.getMount()).append("\n");
        buf.append("path:").append(req.getPath()).append("\n");
        buf.append("remote:").append(req.getRemoteAddress()).append("\n");
        buf.append("protocol:").append(req.getProtocol()).append("\n");
        resp.getEncoder().writeString(buf);
    }
    
}
