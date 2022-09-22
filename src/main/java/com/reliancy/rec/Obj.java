/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.rec;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of a Rec.
 * We separate keys and values because Obj could just be an array.
 * If object is declated an array keys are nonexistant and rec related methods will return null or crash.
 * Our setters return this object to main the calls chainable.
 * Also positional calls accept negative values which reference from end backward.
 */
public class Obj implements Rec{
    final List<Object> values;
    final Hdr meta;

    public Obj() {
        values=new ArrayList<>();
        meta=new Slot(null);
    }
    public Obj(boolean is_array) {
        values=new ArrayList<>();
        meta=new Slot(null);
        if(is_array) meta.raiseFlags(Hdr.FLAG_ARRAY);
    }
    public Obj(List<Slot> k,List<Object> v) {
        values=v;
        meta=new Slot(null);
        meta.keys.addAll(k);
    }
    /**
     * This ctor is reserved for derivations with fixed slot definitions.
     * This constructor will inspect static Slot members and construct keys that way
     * if meta named.
     * @param def
     */
    protected Obj(Hdr def){
        values=new ArrayList<>();
        meta=def;
    }
    @Override
    public String toString(){
        StringBuilder buf=new StringBuilder();
        toString(buf);
        return buf.toString();        
    }
    public int toString(StringBuilder buf){
        boolean is_arr=isArray();
        int length0=buf.length();// length before anything done
        //StringBuffer indent=new StringBuffer(); // detect indent
        //for(int i=length0;i>0 && Character.isWhitespace(buf.charAt(i));i--){
        //    indent.append(buf.codePointAt(i));
        //}
        buf.append(is_arr?"[":"{");
        if(is_arr){
            for(int pos=0;pos<count();pos++){
                if(pos>0) buf.append(",");
                Object val=this.get(pos);
                if(val instanceof Obj) ((Obj)val).toString(buf);
                else if(val!=null) buf.append(val.toString());
                else buf.append("null");
            }
        }else{
            for(int pos=0;pos<count();pos++){
                if(pos>0) buf.append(",");
                Slot s=getSlot(pos);
                buf.append(s.getName()+":");
                Object val=this.get(pos);
                if(val!=null) s.toString(val,buf); else buf.append("null");
            }
        }
        buf.append(is_arr?"]":"}");
        return buf.length()-length0;
    }
    @Override
    public Hdr meta(){
        return meta;
    }
    @Override
    public boolean isArray(){
        return meta==null || meta.checkFlags(Hdr.FLAG_ARRAY);
    }
    @Override
    public int count() {
        return values.size();
    }

    @Override
    public Rec set(int pos, Object val) {
        if(pos<0) pos=count()+pos;
        values.set(pos,val);
        return this;
    }

    @Override
    public Object get(int pos) {
        if(pos<0) pos=count()+pos;
        return values.get(pos);
    }

    @Override
    public Rec add(Object val) {
        values.add(val);
        if(!isArray()) meta.addSlot(new Slot("arg"+count(),Object.class));
        return this;
    }

    @Override
    public Rec remove(int s) {
        values.remove(s);
        if(!isArray()) meta.removeSlot(s);
        return this;
    }


    @Override
    public Rec set(Slot s, Object val) {
        if(s==null) throw new IllegalArgumentException("invalid key provided");
        if(isArray()) throw new IllegalStateException("array not mappable with:"+s.getName());
        int index=s.getPosition(); // try slot position
        if(index<0) index=meta.indexOf(s.getName());// fall back to search if slot not set
        if(index<0){
            values.add(val);
            meta.addSlot(s);
        }else{
            values.set(index,val);
            meta.setSlot(index,s);
        }
        return this;
    }
    /**
     * Returns value by slot key.
     * If the underlying rec is a vec/array this method might work if slot is positioned else it will
     * return def value.
     */
    @Override
    public Object get(Slot s, Object def) {
        if(s==null) throw new IllegalArgumentException("invalid key provided");
        //if(keys==null) throw new IllegalStateException("array not mappable with:"+s.getName());
        int index=s.getPosition(); // try slot position
        if(index<0 && !isArray()) index=meta.indexOf(s.getName());// fall back to search if slot not set
        return index<0?def:values.get(index);
    }

    @Override
    public Rec remove(Slot s) {
        int index=s.getPosition(); // try slot position
        if(index<0 && !isArray()) index=meta.indexOf(s.getName());// fall back to search if slot not set
        if(index>=0) remove(index);
        return this;
    }
    
}
