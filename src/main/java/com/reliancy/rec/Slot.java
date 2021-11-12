package com.reliancy.rec;

/**
 * Slot is a definition of a value start with the name.
 * We use it to define columns/fields of records. 
 * It is also used as header of actual records.
 */
public class Slot extends Hdr {

    public static interface Initializer{
        Object getInitalValue(Slot s,Rec rec);
    }
    public static final Initializer DEFAULT_INITIALIZER=new Initializer(){
        public Object getInitalValue(Slot s,Rec rec) {return s.getInitValue();}
    };
    int position;
    Object defaultValue;
    Initializer initValue;

    public Slot(String name){
        this(name,Object.class);
    }
    public Slot(String name,Class<?> type){
        super(name,type);
        this.position=-1;
        this.initValue=DEFAULT_INITIALIZER;
    }
    public boolean equals(String str){
        return name.equalsIgnoreCase(str);
    }
    public int getPosition() {
        return position;
    }
    public Slot setPosition(int position) {
        this.position = position;
        return this;
    }
    public Object getInitValue() {
        return defaultValue;
    }
    public Slot setInitValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
    public Initializer getInitVia() {
        return initValue;
    }
    public Slot setInitVia(Initializer initValue) {
        this.initValue = initValue;
        return this;
    }
    public int toString(Object val, StringBuilder buf) {
        int length0=buf.length();
        if(val instanceof Obj) ((Obj)val).toString(buf);
        else if(val!=null) buf.append(val.toString());
        else buf.append("null");
        return buf.length()-length0;
    }
    public Object get(Rec r,Object def){
        return r.get(this, def);
    }
    public Slot set(Rec r,Object val){
        r.set(this, val);
        return this;
    }
}
