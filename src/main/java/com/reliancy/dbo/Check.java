/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.util.Iterator;

/** constraint on a field.
 * conditions can be leafs or groups such as and,or,not
 */
public class Check implements Iterable<Check> {
    public static abstract class Op{
        public abstract boolean met(Check c,Object val);
    }
    /** logical AND operation. */
    public static Op AND=new Op(){
        public String toString(){return "AND";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** logical OR operation. */
    public static Op OR=new Op(){
        public String toString(){return "OR";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** logical NOT operation. */
    public static Op NOT=new Op(){
        public String toString(){return "NOT";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** arithmetic equal test. */
    public static Op EQ=new Op(){
        public String toString(){return "=";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** arithmetic negated equal test. */
    public static Op NEQ=new Op(){
        public String toString(){return "<>";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** greater than check. */
    public static Op GT=new Op(){
        public String toString(){return ">";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** greater than or equal check. */
    public static Op GTE=new Op(){
        public String toString(){return ">=";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** less than check. */
    public static Op LT=new Op(){
        public String toString(){return "<";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** less than or equal check. */
    public static Op LTE=new Op(){
        public String toString(){return "<=";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** like check case insensitive. */
    public static Op LIKE=new Op(){
        public String toString(){return "LIKE";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** set membership check. */
    public static Op IN=new Op(){
        public String toString(){return "IN";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** negated set membership check. */
    public static Op NOT_IN=new Op(){
        public String toString(){return "NOT IN";}
        public boolean met(Check c,Object val){
            return true;
        }
    };
    /** iterator over checks. 
     * 
    */
    public static class CheckIterator implements Iterator<Check>{
        final Check root;
        Check cur;
        int index;
        public CheckIterator(Check ch){
            root=ch;
            cur=root;
            index=0;
        }
        @Override
        public boolean hasNext() {
            return cur.isLeaf()==false && index<cur.args.length;
        }

        @Override
        public Check next() {
            return (Check)cur.args[index++];
        }

    }
    public static Check and(Check... c) {
        return new Check(AND,c);
    }
    public static Check all(Check... c) {
        return new Check(AND,c);
    }
    public static Check or(Check... c) {
        return new Check(OR,c);
    }
    public static Check any(Check... c) {
        return new Check(OR,c);
    }
    public static Check not(Check... c) {
        return new Check(NOT,c);
    }
    public static Check none(Check... c) {
        return new Check(NOT,c);
    }
    public static Check eq(Field pk, Object... args) {
        Object id=args;
        if(id!=null && args.length==1) id=args[0];
        return new Check(EQ,pk,id);
    }
    public static Check neq(Field pk, Object... args) {
        Object id=args;
        if(id!=null && args.length==1) id=args[0];
        return new Check(NEQ,pk,id);
    }
    public static Check gt(Field pk, Object... args) {
        Object id=args;
        if(id!=null && args.length==1) id=args[0];
        return new Check(GT,pk,id);
    }
    public static Check gte(Field pk, Object... args) {
        Object id=args;
        if(id!=null && args.length==1) id=args[0];
        return new Check(GTE,pk,id);
    }
    public static Check lt(Field pk, Object... args) {
        Object id=args;
        if(id!=null && args.length==1) id=args[0];
        return new Check(LT,pk,id);
    }
    public static Check lte(Field pk, Object... args) {
        Object id=args;
        if(id!=null && args.length==1) id=args[0];
        return new Check(LTE,pk,id);
    }
    public static Check like(Field pk, Object... args) {
        Object id=args;
        if(id!=null && args.length==1) id=args[0];
        return new Check(LIKE,pk,id);
    }
    public static Check in(Field pk, Object... id) {
        return new Check(IN,pk,id);
    }
    public static Check not_in(Field pk, Object... id) {
        return new Check(NOT_IN,pk,id);
    }
    Op code;
    boolean leaf;
    Object[] args;
    boolean locked;

    public Check(Op code,Field f,Object val){
        this.code=code;
        leaf=true;
        args=new Object[]{f,val};
    }
    public Check(Op code,Check ... sub){
        this.code=code;
        leaf=false;
        args=sub;
    }
    public Check setLocked(boolean f){
        locked=f;
        return this;
    }
    public boolean isLocked(){
        return locked;
    }
    public Op getCode(){
        return code;
    }
    public boolean isLeaf(){
        return leaf;
    }
    public boolean met(Object val){
        return code.met(this,val);
    }
    @Override
    public Iterator<Check> iterator() {
        return new CheckIterator(this);
    }
    public int getChildCount(){
        return leaf?0:args.length;
    }
    public Check getChild(int index){
        return leaf?null:(Check)args[index];    
    }
    public Field getField(){
        return (Field)args[0];
    }
    public Object getValue(){
        return (Object)args[1];
    }
    public Check setValue(Object val){
        if(locked) throw new IllegalStateException("check value is locked");
        if(!leaf) throw new IllegalStateException("check is not a leaf");
        args[1]=val;
        return this;
    }

}
