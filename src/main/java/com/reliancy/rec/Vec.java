package com.reliancy.rec;
/**
 * dimensioned container of values.
 * Our setters return this object to make the calls chainable.
 * Also positional calls accept negative values which reference from end backward.
 * 
 */
public interface Vec {
    public default boolean isArray(){
        return meta().checkFlags(Hdr.FLAG_ARRAY);
    }
    public Hdr meta();
    public int count();
    public Rec set(int pos,Object val);
    public Object get(int pos);
    public Rec add(Object val);
    public Rec remove(int s);
}
