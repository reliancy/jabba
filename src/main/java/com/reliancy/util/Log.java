package com.reliancy.util;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/** Logging support based on JUL.
 * We implement a deferred logmanager that survives shutdownhook until we release.
 */
public class Log {
    static {
        // must be called before any Logger method is used.
        System.setProperty("java.util.logging.manager", DeferredMgr.class.getName());
        System.setProperty("java.util.logging.SimpleFormatter.format","%1$tF %1$tT %4$-7s [%3$s] %5$s%6$s%n");
    }
    public static class DeferredMgr extends LogManager {
        @Override public void reset() { /* don't reset yet. */ }
        private void resetFinally() { super.reset(); }
    }
    public static Logger setup(){
        Logger root_logger=Logger.getLogger("");
        return root_logger;
    }
    public static void cleanup(){
        LogManager mgr=LogManager.getLogManager();
        if(mgr instanceof DeferredMgr){
            ((DeferredMgr)mgr).resetFinally();
        }
    }
    public static void setLevel(Logger logger,String level_name){
        if(level_name==null || level_name.isEmpty()) level_name="ERROR";
        level_name=level_name.toUpperCase();
        switch(level_name){
            case "v":{
                level_name="WARN";
                break;
            }
            case "vv":{
                level_name="INFO";
                break;
            }
            case "vvv":{
                level_name="DEBUG";
                break;
            }
        }
        switch(level_name){
            case "WARN":{
                level_name="WARNING";
                break;
            }
            case "DEBUG":{
                level_name="FINER";
                break;
            }
            case "ERROR":{
                level_name="SEVERE";
                break;
            }
        }
        Level lvl=Level.parse(level_name);
        logger.setLevel(lvl);
        for (Handler h : logger.getHandlers()) {
            h.setLevel(lvl);
        }        
    }
    
}
