package com.reliancy.rec;

import java.util.ArrayList;
import java.util.ListIterator;

/** Base class of meta objects.
 * We use it to describe certain meta information. We derive from it Slot.
 * We define keys list of slots on the header level to describe slots.
 */
public class Hdr {
    public static final int FLAG_ARRAY      =0x0001;
    public static final int FLAG_CHANGED    =0x0002;
    public static final int FLAG_HIDDEN     =0x0004;
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
        ret.append("{").append("flags:").append(flags).append(",name:").append(name);
        ret.append(",dim:").append(keys.size()).append("}");
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
    public int findSlot(String name){
        return findSlot(name,0);
    }
    public int findSlot(String name,int ofs){
        ListIterator<Slot> it=keys.listIterator(ofs);
        while(it.hasNext()){
            int index=it.nextIndex();
            Slot e=it.next();
            if(e.getName().equalsIgnoreCase(name)) return index;
        }
        return -1;
    }
    public int findSlot(Slot s,int ofs){
        ListIterator<Slot> it=keys.listIterator(ofs);
        while(it.hasNext()){
            int index=it.nextIndex();
            Slot e=it.next();
            if(e==s) return index;
        }
        return -1;
    }
        /**
     * this version will get or create a slot by given name.
     * @param name
     * @return
     */
    public Slot getSlot(String name){
        int index=findSlot(name);
        if(index<0){
             return new Slot(name);
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
    public Slot[] slots(Slot... slots){
        if(slots!=null && slots.length>0){
            keys.clear();
            for(int i=0;i<slots.length;i++) keys.add(slots[i]);
        }
        return keys.toArray(new Slot[keys.size()]);
    }
}
