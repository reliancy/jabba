package com.reliancy.jabba;

import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
/**
 * Unit test for simple App.
 */
public class RouterTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        Pattern p=Pattern.compile("(/hello)|(/hello2(/c))|(/hello3)");
        Matcher m=p.matcher("/hello2/c");
        if(m.matches()){
            for(int i=0;i<m.groupCount();i++){
                System.out.println(i+":"+m.group(i));
            }
        }
        assertTrue( true );
    }
    @Test
    public void initRouter()
    {
        //assertTrue( true );
        System.out.println("Test router init...");
        Router r=new Router();
        RouterEndPoint rep=r.importEndPoints(r);
        rep.compile();
        //Matcher m=rep.match("GET","/helloPlain");
        Matcher m=rep.match("GET","/hello3/45");
        //Matcher m=rep.match("GET","/helloP");
        if(m!=null){
            HashMap<String,String> pms=new HashMap<>();
            String rt=rep.evalMatcher(m,pms);
            System.out.println(rt);
            System.out.println(pms);
        }
    }
}
