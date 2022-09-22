/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Helper or model class to manage menus and toolbars.
 * 
 */
public class Menu extends MenuItem{
    public static final String TOP="/menu/top";
    public static final String LEFT="/menu/left";

    public static final HashMap<String,Menu> menus=new HashMap<>();
    public static void publish(String name,Menu m){
        menus.put(name,m);
    }
    public static Menu request(String name){
        Menu ret=menus.get(name);
        if(ret==null){
            ret=new Menu(name);
            publish(name,ret);
        }
        return ret;
    }
    final ArrayList<MenuItem> items=new ArrayList<>();
    public Menu(String id) {
        super(id);
    }
    public int getSize(){
        return items.size();
    }
    public Menu add(MenuItem itm){
        items.add(itm);
        return this;
    }
    public Menu addSpacer(){
        return add(new MenuItem("###"));
    }
    public MenuItem find(String id){
        if(id.equalsIgnoreCase(getId())) return this;
        for(MenuItem itm:items) if(id.equalsIgnoreCase(itm.getId())) return itm;
        return null;
    }
    public List<MenuItem> getItems() {
        return items;
    }
    public Iterable<MenuItem> getBefore() {
        final ArrayList<MenuItem> ret=new ArrayList<>();
        for(MenuItem itm:items){
            if("###".equals(itm.getId())) break;
            ret.add(itm);
        }
        return ret;
    }
    public Iterable<MenuItem> getAfter() {
        final ArrayList<MenuItem> ret=new ArrayList<>();
        boolean adding=false;
        for(MenuItem itm:items){
            if("###".equals(itm.getId())){
                adding=true;
            }else if(adding){
                ret.add(itm);
            }
        }
        return ret;
    }
    
}
