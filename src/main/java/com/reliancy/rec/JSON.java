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


}
