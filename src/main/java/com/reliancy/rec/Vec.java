/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
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
