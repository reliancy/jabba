package com.reliancy.jabba;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class ArgsConfigTest {
    @Test
    public void testEnv(){
        System.out.println("testing sys env...");
        ArgsConfig args=new ArgsConfig("prog","--verbose","--key","value","cmd");
        try {
            args.load();
            ArgsConfig.Property<String> env_user=new ArgsConfig.Property<>("USER",String.class);
            ArgsConfig.Property<String> sys_user=new ArgsConfig.Property<>("user.name",String.class);
            ArgsConfig.Property<Boolean> verbose=new ArgsConfig.Property<>("verbose",Boolean.class);
            String usr_val1=args.getProperty(env_user,"None1");
            String usr_val2=args.getProperty(sys_user,"None2");
            System.out.println("Env User:"+usr_val1);
            System.out.println("Sys User:"+usr_val2);
            assertTrue(usr_val1.equals(usr_val2));
            System.out.println("Positional:"+args.getProperty(Config.APP_ARGS, null));
            System.out.println("Verbose:"+verbose.get(args));
            for(ArgsConfig.Property<?> p:args){
                System.out.println("p:"+p+"="+p.get(args));
            }

        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }    
}
