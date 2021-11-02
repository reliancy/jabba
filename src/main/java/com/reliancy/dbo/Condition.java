package com.reliancy.dbo;

/** constraint on a field.
 * conditions can be leafs or groups such as and,or,not
 */
public class Condition {
    public static abstract class Op{
        public abstract boolean met(Condition c);
    }
    public static Op AND=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op OR=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op NOT=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op EQ=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op NEQ=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op GT=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op GTE=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op LT=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op LTE=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op LIKE=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Op IN=new Op(){
        public boolean met(Condition c){
            return true;
        }
    };
    public static Condition and(Condition... c) {
        return new Condition(AND,c);
    }
    public static Condition eq(Field pk, Object... id) {
        return new Condition(EQ,pk,id);
    }
    Op code;
    Object[] args;
    public Condition(Op code,Field f,Object val){
        this.code=code;
        args=new Object[]{f,val};
    }
    public Condition(Op code,Condition ... sub){
        this.code=code;
        args=sub;
    }
}
