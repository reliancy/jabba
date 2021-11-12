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
