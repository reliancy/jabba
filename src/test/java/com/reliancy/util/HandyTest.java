/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.util;
import java.io.IOException;

import org.junit.Test;

public class HandyTest {
    /**
     * Plain CRUD
     * @throws IOException
     */
    @Test
    public void splitting() throws IOException
    {
        System.out.println("Splitting test...");
        String tst1="One,Two,Three";
        System.out.println(tst1+" over ,");
        for(String s:Handy.split(",",tst1)){
            System.out.println("\tt:"+s);
        }
        //System.out.println(tst1+" over ");
        //for(String s:Handy.split("",tst1)){
        //    System.out.println("\tt:"+s);
        //}
        String tst2="AND A AND B ANDAND D AND";
        System.out.println(tst2+" over AND");
        for(String s:Handy.split("AND",tst2)){
            System.out.println("\tt:"+s);
        }

    }
    
}
