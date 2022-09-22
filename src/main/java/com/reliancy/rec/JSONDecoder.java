/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.rec;

import java.util.LinkedList;

import com.reliancy.util.Tokenizer;
import com.reliancy.util.Handy;

/** Special class which will tokenize string according to rules for JSON and feed the info to a listener.
 * TODO: reuse headers in an array if same structure
 * @author amer
 */
public class JSONDecoder implements TextDecoder,DecoderSink {
	DecoderSink handler;
	String[] inBody;
	String[] sets;
	String lastToken=null;
	StringBuilder out = new StringBuilder();
	public JSONDecoder(DecoderSink h){
		handler=h;
		String delimChars="{}[],;:=";
		String escapeChars="'\"";
		String whiteChars=" \t\r\f\n";//" \t\r\f\n";
		inBody = new String[]{delimChars,escapeChars,whiteChars};
		sets=inBody;
	}
	public JSONDecoder(){
		this(null);
		handler=this;
	}
	@Override
	public int parse(int offset,CharSequence in){
		int noffset=0;
		while((noffset = Tokenizer.nextToken(offset, in, out, sets))!=offset){
			offset=noffset;
			if(out.length()==0) continue;
			String token=out.toString();
			out.setLength(0);
			if("{".equals(token)){
				if(lastToken!=null){
					if(lastToken.startsWith("/*") || lastToken.startsWith("//")){
					handler.setValue(lastToken); // support comments in our stream
					}else{
					handler.setKey(lastToken); // we consider string before { a key or name unless comment
					}
					lastToken=null;
				}
				handler.beginElement("object");
			}else if("}".equals(token)){
				if(lastToken!=null){
					handler.setValue(lastToken);
					lastToken=null;
				}
				handler.endElement("object");
			}else if("[".equals(token)){
				if(lastToken!=null){
					handler.setValue(lastToken);
					lastToken=null;
				}
				handler.beginElement("array");
			}else if("]".equals(token)){
				if(lastToken!=null){
					handler.setValue(lastToken);
					lastToken=null;
				}
				handler.endElement("array");
			}else if(",".equals(token) || ";".equals(token)){
				if(lastToken!=null){
					handler.setValue(lastToken);
					lastToken=null;
				}
			}else if(":".equals(token) || "=".equals(token)){
				if(lastToken!=null){
					handler.setKey(lastToken);
					lastToken=null;
				}
			}else{
				lastToken=token;
			}
		}
		if(lastToken!=null){
			handler.setValue(lastToken);
			lastToken=null;
		}
		return offset;
	}
	
	Slot KEY=new Slot("__key",String.class);
	/** We use a stack structure to manage recusion. */
	LinkedList<Rec> stack=new LinkedList<Rec>();
	/** will not add white space only nodes. */
	boolean whitespaceIgnored=true;
	boolean entitycharsIgnored=false;

	public boolean isWhitespaceIgnored() {
		return whitespaceIgnored;
	}

	public void setWhitespaceIgnored(boolean whitespaceIgnored) {
		this.whitespaceIgnored = whitespaceIgnored;
	}

	public boolean isEntitycharsIgnored() {
		return entitycharsIgnored;
	}

	public void setEntitycharsIgnored(boolean entitycharsIgnored) {
		this.entitycharsIgnored = entitycharsIgnored;
	}

    public Rec getRoot() {
        return stack.getLast();
    }
	public Rec getSubject(){
		if(stack.isEmpty()) return null;
		return stack.getFirst();
	}
	public void pushSubject(Rec n){
		stack.push(n);
	}
	public Rec popSubject(){
		Rec child=stack.pop();
		Rec parent=getSubject();
		if(parent==null) return child;
		if(parent.isArray()){
			parent.add(child);
		}else{
			String key=(String) parent.get(KEY,null);
			Slot keyslot=parent.getSlot(key);
			parent.remove(KEY).set(keyslot,child);
			// if array and has key it should bomb
			//parent.setArray(false);
		}
		return child;	
	}
	
	public void beginDocument() {
		beginDocument(null);
	}
	@Override
	public void beginDocument(Rec init) {
		sets=inBody;
		out.setLength(0);
		lastToken=null;
        stack.clear();
		Rec arr=new Obj(true);
		stack.push(arr);
		//System.out.println("BeginDoc");
	}

	@Override
	public Rec endDocument() {
        // need to set the actual parent
		while(stack.getFirst()!=stack.getLast()){
			popSubject();
		}
		// now adjust the root if it is array with only one child - one we added in start document as first element
		Rec root=getSubject();
		if(root.isArray() && root.count()==1 && root.get(0) instanceof Rec){
			// ok we collapse our array from above - since we only have one object
			Object bb=root.get(0);
			Rec b=(Rec)bb ;
			popSubject();
			pushSubject(b);
		}
		//System.out.println("EndDoc");
		return getRoot();
	}

	@Override
	public void beginElement(String name) {
        Rec element=new Obj("array".equals(name));
        //element.setAttr(0);
		pushSubject(element);
		//System.out.println("BeginElement:"+name);
	}

	@Override
	public void endElement(String name) {
        // check if the correct end element is sent
		Rec sub=this.getSubject();
		if(!sub.isArray()) sub.remove(KEY);
        // finally pop the root
        popSubject();
		//System.out.println("EndElement:"+name);
	}

	@Override
	public void setKey(String name) {
		Rec sub=this.getSubject();
		String key=(String) sub.get(KEY,null);
		if(key!=null){
			// something is wrong - our tokizer might have ignored escape char or input has forgotten a delimiter
			// we try to split name because it would contain key and value merged
			int split=0;
			if(name.startsWith("\"")) split=name.indexOf('\"', 1);
			if(name.startsWith("'")) split=name.indexOf('\'', 1);
			String val=name.substring(0,split+1);
			setValue(val);
			name=name.substring(split+1);
		}
		int start=0;int stop=name.length();
		while(start<stop && (name.charAt(start)=='"' || name.charAt(start)=='\'')) start++;
		while(start<stop && (name.charAt(stop-1)=='"' || name.charAt(stop-1)=='\'')) stop--;
		sub.set(KEY, name.subSequence(start, stop));
		//System.out.println("BeginAttribute:"+name);
	}

	@Override
	public void setValue(CharSequence seq) {
		if(seq==null) return;
		Rec sub=this.getSubject();
		String key=(String) sub.get(KEY,null);
		if(key==null){
			if(isWhitespaceIgnored() && Handy.isEmpty(seq)){
				// skip empty strings
				return;
			} 
			// now key we are adding to body
			Object val=interpretString(seq);
			sub.add(val);
		}else{
			// we are setting attribute
			Object val=interpretString(seq);
			Slot keyslot=sub.getSlot(key);
			sub.remove(KEY).set(keyslot,val);
			// it should bomb if array and comes with key
			//sub.setArray(false); // if it needs to be array why does it have a key
		}
		//System.out.println("Data:"+seq);
	}
	public Object interpretString(CharSequence seq){
			int start=0;int stop=seq.length();
			while(start<stop && seq.charAt(start)=='"' && seq.charAt(stop-1)=='"'){
				start++;
				stop--;
			}
			if(start==0 && stop==seq.length()){
				// we do not trim single quotes unless double are missing
				while(start<stop && seq.charAt(start)=='\'' && seq.charAt(stop-1)=='\''){
					start++;
					stop--;
				}
			}
			seq=seq.subSequence(start, stop);
			Object val=seq;
			if(start==0){
				String sVal=String.valueOf(seq);
				// we did not have quotes - so try to interpet a few things
				if("null".equalsIgnoreCase(sVal)){
					val=null;
				}else
				if("true".equalsIgnoreCase(sVal)){
					val=Boolean.TRUE;
				}else	
				if("false".equalsIgnoreCase(sVal)){
					val=Boolean.FALSE;
				}else	
				if(Handy.isNumeric(sVal)){
					if (sVal.indexOf(".") >= 0) {
						val = Double.parseDouble(sVal);
					} else {
						val = Integer.parseInt(sVal);
					}
				}else if(this.isEntitycharsIgnored()==false && seq!=null && seq.length()>0){
					// maybe it is a string after all
					val=unescape(seq);
				}
			}else if(this.isEntitycharsIgnored()==false && seq!=null && seq.length()>0){
				// we had quotes so lets decode escaed chars
				val=unescape(seq);
			}
			return val;
	}

	public static CharSequence unescape(CharSequence str) {
		StringBuilder buf = null;
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (ch == '\\' && i < (str.length() - 1)) {
				i = i + 1;
				char ch2 = str.charAt(i);
				switch (ch2) {
					case '"':
						if(buf==null) buf=new StringBuilder(i>0?str.subSequence(0, i-1):"");
						buf.append("\"");
						break;
					case '\\':
						if(buf==null) buf=new StringBuilder(i>0?str.subSequence(0, i-1):"");
						buf.append("\\");
						break;
					case '/':
						if(buf==null) buf=new StringBuilder(i>0?str.subSequence(0, i-1):"");
						buf.append("/");
						break;
					case 'b':
						if(buf==null) buf=new StringBuilder(i>0?str.subSequence(0, i-1):"");
						buf.append("\b");
						break;
					case 'f':
						if(buf==null) buf=new StringBuilder(i>0?str.subSequence(0, i-1):"");
						buf.append("\f");
						break;
					case 'n':
						if(buf==null) buf=new StringBuilder(i>0?str.subSequence(0, i-1):"");
						buf.append("\n");
						break;
					case 'r':
						if(buf==null) buf=new StringBuilder(i>0?str.subSequence(0, i-1):"");
						buf.append("\r");
						break;
					case 't':
						if(buf==null) buf=new StringBuilder(i>0?str.subSequence(0, i-1):"");
						buf.append("\t");
						break;
					default:
						if(buf!=null) buf.append(ch);
				}
			} else {
				if(buf!=null) buf.append(ch);
			}
		}
		return buf!=null?buf.toString():str;
	}
	
}
