package com.reliancy.rec;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class ObjTest {
    /**
     * Plain CRUD
     * @throws IOException
     */
    @Test
    public void crudVec() throws IOException
    {
        Obj o=new Obj();
        Obj a=new Obj(true);
        System.out.println("O1:"+o);
        System.out.println("A1:"+a);
        a.add(1).add("three");
        o.add(1).add("three").set(new Slot("arr"),new String[]{"a","b","c"});
        System.out.println("O2meta:"+o.isArray()+"/"+o.meta());
        System.out.println("O2:"+o);
        System.out.println("A2:"+a);
        o.set(o.getSlot("car"),"bar");
        System.out.println("O3:"+o);
        StringBuilder json=new StringBuilder();
        JSON.writes(o,json);
        System.out.println("ENC:"+json);
        Rec dec=JSON.reads(json);
        System.out.println("DEC:"+dec);
    }
    
}
