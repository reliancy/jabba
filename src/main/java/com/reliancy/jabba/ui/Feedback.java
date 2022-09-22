/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.jabba.ui;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import com.reliancy.jabba.AppSession;

/**
 * List of Feedback events with siphon like iterator.
 * When iterating over the list it pops values as well.
 */
public class Feedback extends LinkedList<FeedbackLine>{
    public static Feedback get(){
        AppSession ass=AppSession.getInstance();
        return ass!=null?ass.getFeedback():null;
    }
    class Siphon implements Iterator<FeedbackLine>{
        final ListIterator<FeedbackLine> backend;
        public Siphon(ListIterator<FeedbackLine> it){
            backend=it;
        }
        @Override
        public boolean hasNext() {
            return backend.hasNext();
        }
        @Override
        public FeedbackLine next() {
            FeedbackLine ret=backend.next();
            backend.remove();
            return ret;
        }
    }
    @Override
    public Iterator<FeedbackLine> iterator(){
        return new Siphon(this.listIterator());
    }

}
