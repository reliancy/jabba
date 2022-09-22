/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.dbo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Observable;

/** A more or less virtual collection of items.
 * this object is a suitable holder of resultsets. it will be overridable so
 * we can create specialized virtual holders that use backends. by itself it will
 * implement in memory list.
 * also this class is an observable and we can monitor update to it.
 */
public class Bag<E> extends Observable implements Collection<E>{
    /** event to send to observers. */
    public static final class BagChanged<E>{
        public static final int ADD=0;
        public static final int REMOVE=1;
        public static final int ACCESS=2;
        public static final int POST_LOAD=3;
        public static final int PRE_SAVE=4;
        final Bag<E> bag;
        final int operation;
        final Object[] arguments;
        public BagChanged(Bag<E> p,int op,Object ... args){
            bag=p;
            operation=op;
            arguments=args;
        }
        public Bag<E> getBag() {
            return bag;
        }
        public int getOperation() {
            return operation;
        }
        public Object[] getArguments() {
            return arguments;
        }
    }
    final ArrayList<E> items=new ArrayList<>();
    
    public Bag(){
    }
    public Bag(Iterable<E> o){
        this(o.iterator());
    }
    public Bag(Iterator<E> o){
        while(o.hasNext()) add(o.next());
    }
    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public boolean contains(Object o) {
        final Iterator<E> it=iterator();
        while(it.hasNext()){
            final E e=it.next();
            if(e!=null && o!=null && e.equals(o)) return true;
            else if(e==o) return true;
        }
        return false;
    }
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c) if (!contains(e)) return false;
        return true;
    }
    public ListIterator<E> listIterator(){
        return listIterator(0);
    }
    public ListIterator<E> listIterator(int offset){
        return items.listIterator(offset);
    }
    @Override
    public Iterator<E> iterator() {
        return items.iterator();
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return items.toArray(a);
    }

    @Override
    public boolean add(E e) {
        if(items.contains(e)) return true;
        if(countObservers()>0){
            BagChanged<E> evt=new Bag.BagChanged<>(this,BagChanged.ADD,e);
            setChanged();
            notifyObservers(evt);
        }
        return items.add(e);
    }
    public Bag<E> append(E e){
        add(e);
        return this;
    }
    @Override
    public boolean remove(Object o) {
        if(!contains(o)) return false;
        if(countObservers()>0){
            BagChanged<E> evt=new Bag.BagChanged<>(this,BagChanged.REMOVE,o);
            setChanged();
            notifyObservers(evt);
        }
        return items.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if(countObservers()>0){
            BagChanged<E> evt=new Bag.BagChanged<>(this,BagChanged.ADD,c.toArray());
            setChanged();
            notifyObservers(evt);
        }
        if(c==null || c.size()==0) return false;
        c.forEach(e->{this.append(e);});
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if(countObservers()>0){
            BagChanged<E> evt=new Bag.BagChanged<>(this,BagChanged.REMOVE,c!=null?c.toArray():null);
            setChanged();
            notifyObservers(evt);
        }
        if(c!=null){
            return items.removeAll(c);
        }else{
            items.clear();
            return true;
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return items.retainAll(c);
    }

    @Override
    public void clear() {
        removeAll(null);
    }
    
}
