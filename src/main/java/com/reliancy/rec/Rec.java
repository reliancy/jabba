/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.rec;

/**
 * A record representation like in JSON.
 * This is either an array or a map of fields.
 * Each field definition we call a slot.
 */
public interface Rec extends Vec{
    public Rec set(Slot s,Object val);
    public Object get(Slot s,Object def);
    public Rec remove(Slot s);
    public default Slot getSlot(String name){
        Hdr m=meta();
        return m!=null?m.getSlot(name,true):null;
    }
    public default Slot getSlot(int pos){
        Hdr m=meta();
        return m!=null?m.getSlot(pos):null;
    }
}
