package com.reliancy.jabba;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class FileConfigTest {
    @Test
    public void testFile(){
        System.out.println("testing file config...");
        ArgsConfig args0=null;//new ArgsConfig();
        FileConfig args=new FileConfig(args0,"./var/conf.ini");
        try {
            args.load();
            for(ArgsConfig.Property<?> p:args){
                System.out.println("p:"+p+"="+p.get(args));
            }

        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }    
}
