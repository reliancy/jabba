/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.rec;
import java.io.IOException;

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
