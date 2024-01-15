/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.github.jknack.handlebars.Context;
import com.reliancy.jabba.Response;
import com.reliancy.util.CodeException;

/**
 * Data context and render task for our handlebars UI system.
 * We create a rendering for a template, load it with data and then flush it at end into a document.
 * In this class we also inject some global variables such as menu, feedback and user.
 */
public class Rendering extends HashMap<String,Object>{
    public static Rendering begin(Template t){
        Rendering ret=new Rendering(t);
        ret.with("menu",Menu.request(Menu.TOP));
        ret.with("toolbar",Menu.request(Menu.LEFT));
        ret.with("feedback",Feedback.get());
        ret.with("layout","land-app");
        return ret;
    }
    public static Rendering begin(String path,Object ...sp){
        //if(sp==null ||sp.length==0) sp=new Object[]{"./var",App.class};
        Template t=Template.find(path,sp);
        if(t==null) throw new CodeException(Template.ERR_BADTEMPLATE).put("template",path);
        return begin(t);
    }
    Context ctx;
    Template template;
    public Rendering(Template t){
        ctx=Context.newBuilder(this).build();
        template=t;
    }
    public CharSequence end() throws IOException{
        try{
            CharSequence ret=template.render(ctx);
            return ret;
        }finally{
            ctx.destroy();
        }   
    }
    public void end(Response resp) throws IOException{
        try{
            template.render(ctx,resp.getEncoder().getWriter());
        }finally{
            ctx.destroy();
        }   
    }
    public Rendering with(String key,Object val){
        put(key,val);
        return this;
    }
    public Rendering with(Map<String,?> kv){
        for(Map.Entry<String,?> e:kv.entrySet()){
            put(e.getKey(),e.getValue());
        }
        return this;
    }
    public Rendering with(Throwable ex){
        StringBuilder msg=new StringBuilder();
        StringBuilder title=new StringBuilder();
        CodeException.fillUserMessage(ex, msg, title);
        with("error_title",title.toString());
        with("error_message",msg);
        return this;
    }

}
