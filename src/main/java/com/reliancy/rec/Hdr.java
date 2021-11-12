package com.reliancy.rec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Base class of meta objects.
 * We use it to describe certain meta information. We derive from it Slot.
 * We define keys list of slots on the header level to describe sub-slots.
 * 
 * This class describes structure of Fields or Entities via the keys array of slots.
 * Additionally we provide a number of methods to locate, set or get or remove or add slots.
 * However slots could reside in other places such as base classes and so getOwnSlots will return a 
 * bare list of slots in this object while all other methods will take into account other sources.
 * We do this to stay consistent at Rec level when Hdr inheritance comes into play.
 */
public class Hdr {
    public static final int FLAG_ARRAY      =0x0001;
    public static final int FLAG_CHANGED    =0x0002;
    public static final int FLAG_STORABLE   =0x0004;
    public static final int FLAG_LOCKED     =0x0008;
    int flags;
    String name;
    String label;
    Class<?> type;
    final ArrayList<Slot> keys;
    
    public Hdr(String name) {
        this.name=name;
        keys=new ArrayList<>();
    }
    public Hdr(String name,Class<?> type) {
        this.name=name;
        this.type=type;
        keys=new ArrayList<>();
    }
    @Override
    public String toString(){
        StringBuilder ret=new StringBuilder();
        ret.append(name).append(":");
        ret.append("{")
            .append("flags:").append(flags)
            .append(",dim:").append(count())
            .append("}");
        return ret.toString();
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getLabel() {
        return label!=null?label:name;
    }
    public void setLabel(String name) {
        this.label = name;
    }
    public Class<?> getType() {
        return type;
    }
    public void setType(Class<?> type) {
        this.type = type;
    }
    public int getFlags(){
        return flags;
    }
    public Hdr raiseFlags(int f){
        flags|=f;
        return this;
    }
    public Hdr clearFlags(int f){
        flags&=~f;
        return this;
    }
    public boolean checkFlags(int f){
        return (flags & f)!=0;
    }
    public <T extends Hdr> T castAs(Class<T> clazz){
        return clazz.cast(this);
    }
    public List<Slot> getOwnSlots(){
        return keys;
    }
    public boolean isOwned(Slot s){
        return keys.contains(s);
    }
    public Iterator<Slot> iterator(int offset){
        return keys.listIterator(offset);
    }
    public int indexOf(String name){
        return indexOf(name,0);
    }
    public int indexOf(String name,int ofs){
        Iterator<Slot> it=iterator(ofs);
        int index=-1;
        while(it.hasNext()){
            index+=1;
            Slot e=it.next();
            //if(e.getName().equalsIgnoreCase(name)) return index;
            if(e.equals(name)) return index;
        }
        return -1;
    }
    public int indexOf(Slot s,int ofs){
        Iterator<Slot> it=iterator(ofs);
        int index=-1;
        while(it.hasNext()){
            index+=1;
            Slot e=it.next();
            if(e==s) return index;
        }
        return -1;
    }
    public Slot makeSlot(String name){
        return new Slot(name);
    }
    /**
     * this version will get or create a slot by given name.
     * @param name
     * @return
     */
    public Slot getSlot(String name,boolean make){
        int index=indexOf(name);
        if(index<0){
             return make?makeSlot(name):null;
        }else{
            return getSlot(index);
        }
    }
    public Slot getSlot(int pos){
        return keys.get(pos);
    }
    public Hdr removeSlot(int pos){
        keys.remove(pos);
        return this;
    }
    public Hdr addSlot(Slot s){
        keys.add(s);
        return this;
    }
    public Hdr setSlot(int index,Slot s){
        keys.set(index,s);
        return this;
    }
    public int count(){
        return keys.size();
    }
}
