package com.reliancy.jabba;

public interface Config {
    public void load();
    public void save();
    public String getId();
    public Object getProperty(String key,Object def);
    public Config setProperty(String key,Object val);
}
