package com.reliancy.dbo;

import com.reliancy.rec.Slot;
/**
 * Description of a column or property.
 */
public class Field extends Slot {

    public Field(String name) {
        super(name);
    }
    public Field(String name,Class<?> typ) {
        super(name,typ);
    }
    
}
