package com.reliancy.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Chains multiple iterators to act as one.
 * 
 */
public class JointIterator<T> implements Iterator<T> {
    final Iterator<T> iterators[];
    int cursor;
    @SafeVarargs
    public JointIterator(Iterator<T> ...its){
        this.iterators=its;
        cursor=0;
    }
    @Override
    public boolean hasNext() {
        while(cursor<iterators.length){
            if(iterators[cursor].hasNext()) return true;
            cursor+=1; // cursor exhausted got to next iterator
        }
        return false;
    }

    @Override
    public T next() {
        if(cursor<iterators.length){
            return iterators[cursor].next();
        }else{
            throw new NoSuchElementException();
        }
    }
    
}
