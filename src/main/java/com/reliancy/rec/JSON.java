/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.rec;

import java.io.IOException;

/**
 * Static methods related to JSON format.
 */
public class JSON {
    private JSON(){
    }
    public static final Rec reads(CharSequence seq){
        JSONDecoder dec=new JSONDecoder();
        dec.beginDocument();
        dec.parse(0, seq);
        return dec.endDocument();
    }
    public static final void writes(Rec rec,Appendable sink) throws IOException{
        JSONEncoder.encode(rec, sink);
    }
    public static final String toString(Rec rec){
        StringBuffer buf=new StringBuffer();
        try {
            writes(rec,buf);
        } catch (IOException e) {
        }
        return buf.toString();
    }

}
