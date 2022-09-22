/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import com.reliancy.rec.Slot;
/**
 * Description of a column or property.
 */
public class Field extends Slot {
    public static Field Int(String name){
        return new Field(name,Integer.class);
    }
    public static Field Str(String name){
        return new Field(name,String.class);
    }
    public static Field Bool(String name){
        return new Field(name,Boolean.class);
    }
    public static Field Float(String name){
        return new Field(name,Float.class);
    }
    public static Field Num(String name){
        return new Field(name,BigDecimal.class);
    }
    public static Field Date(String name){
        return new Field(name,Date.class);
    }
    public static Field DateTime(String name){
        return new Field(name,Timestamp.class);
    }
    public static final int FLAG_PK         =0x0100;
    public static final int FLAG_AUTOINC    =0x0200;
    String id;
    String typeParams;
    public Field(String name) {
        super(name);
        this.raiseFlags(Field.FLAG_STORABLE);
    }
    public Field(String name,Class<?> typ) {
        super(name,typ);
        this.raiseFlags(Field.FLAG_STORABLE);
    }
    @Override
    public boolean equals(String str){
        return super.equals(str) || (id!=null && id.equalsIgnoreCase(str));
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public boolean isPk() {
        return checkFlags(FLAG_PK);
    }
    public Field setPk(boolean pk) {
        if(pk) raiseFlags(FLAG_PK); else clearFlags(FLAG_PK);
        return this;
    }
    public boolean isAutoIncrement() {
        return checkFlags(FLAG_AUTOINC);
    }
    public Field setAutoIncrement(boolean pk) {
        if(pk) raiseFlags(FLAG_AUTOINC); else clearFlags(FLAG_AUTOINC);
        return this;
    }
    public String getTypeParams() {
        return typeParams;
    }
    public Field setTypeParams(String p) {
        typeParams=p;
        return this;
    }
    public Check eq(Object... val) {
        return Check.eq(this,val);
    }
    public Check neq(Object... val) {
        return Check.neq(this,val);
    }
    public Check gt(Object... val) {
        return Check.gt(this,val);
    }
    public Check gte(Object... val) {
        return Check.gte(this,val);
    }
    public Check lt(Object... val) {
        return Check.lt(this,val);
    }
    public Check lte(Object... val) {
        return Check.lte(this,val);
    }
    public Check like(Object... val) {
        return Check.like(this,val);
    }
    public Check in(Object... val) {
        return Check.in(this,val);
    }
    
}
