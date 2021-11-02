package com.reliancy.util;

import java.util.ArrayList;
import java.util.Iterator;

/** A utility to help us tokenize text along delimChars.
 * This class is a little better than the java version because it allows for escaped delimChars.
 * Delimiters are escaped with a slash, also single and double quotes supress delimiting when encountered.
 * @author amer
 */
public class Tokenizer implements Iterable<String>,Iterator<String>{
	public static final String WHITECHARS=" \t\r\f\n";
	public static final String DELIMCHARS=" ,:;=<>{}[]()";
	int offset;
	CharSequence input;
	String delimChars=DELIMCHARS;
	String escapeChars="'\"";
	String whiteChars=WHITECHARS;
	public Tokenizer(CharSequence input){
		this.input=input;
	}
	public Tokenizer(CharSequence input,int offset){
		this.input=input;
		this.offset=offset;
	}

	public CharSequence getInput() {
		return input;
	}

	public int getOffset() {
		return offset;
	}

	public Tokenizer setOffset(int offset) {
		this.offset = offset;
		return this;
	}

	public Tokenizer setInput(CharSequence input) {
		this.input = input;
		return this;
	}
	public boolean hasMoreTokens(){
		if(offset>=input.length()) return false;
		for(int i=offset;i<input.length();i++){
			char ch=input.charAt(i);
			if(Tokenizer.isElementOf(ch,whiteChars)==-1) return true;
		}
		return false;
	}
	public String nextToken(){
		final StringBuilder out=new StringBuilder();
		if(nextToken(out)){
			return out.toString();
		}else{
			return null;
		}
	}
	public boolean nextToken(StringBuilder out){
		String[] sets={delimChars,escapeChars,whiteChars};
		int noffset=nextToken(offset,input,out,sets);
		if(noffset==offset){
			return false;
		}else{
			offset=noffset;
			return true;
		}
	}

	public String getDelimChars() {
		return delimChars;
	}

	public Tokenizer setDelimChars(String delimChars) {
		this.delimChars = delimChars;
		return this;
	}

	public String getEscapeChars() {
		return escapeChars;
	}

	public Tokenizer setEscapeChars(String escapeChars) {
		this.escapeChars = escapeChars;
		return this;
	}

	public String getWhiteChars() {
		return whiteChars;
	}

	public Tokenizer setWhiteChars(String whiteChars) {
		this.whiteChars = whiteChars;
		return this;
	}
	/**
	 * Utility method which collects all tokens and returns an array of them.
	 * Use it for small length strings when parsing user input. 
	 * @param withdelims if tru returns delimiters as well
	 */
	public String[] getTokens(boolean withdelims){
		final ArrayList<String> buf=new ArrayList<String>();
		final StringBuilder out=new StringBuilder();
		boolean lastSkipped=false;
		while(this.nextToken(out)){
			String tok=out.toString();
			out.setLength(0);
			if(!withdelims && tok.length()==1 && isElementOf(tok.charAt(0),delimChars)!=-1){
				if(lastSkipped) buf.add("");
				lastSkipped=true;
				continue;
			}
			buf.add(tok);
			lastSkipped=false;
		}
		return buf.toArray(new String[buf.size()]);
	}
	@Override
	public Iterator<String> iterator() {
		return this;
	}
	@Override
	public boolean hasNext() {
		return this.hasMoreTokens();
	}

	@Override
	public String next() {
		return this.nextToken();
	}
	public static int isElementOf(char ch,String d){
		if(d==null) return -1;
		return d.indexOf(ch);
	}
	/**Returns the next token and updated offset.
	 * This is an inline tokenizer for text parsing and the workhorse of the class.
	 * It stops when it encounters a delimiter. It treats delimChars as tokens too.
	 * It advances the offset whenever it was able to move be it delimiter or not.
	 * We should not have to adjust it for repeated calls except for special cases.
	 * @param offset
	 * @param sets various char sets 0-delimiters,1-escape chars,3-white chars
	 * @param input input chars
	 * @param out value of the token
	 * @return offset after processing
	 */
	public static int nextToken(int offset,CharSequence input, StringBuilder out,String[] sets){
		String delimChars=(sets!=null && sets.length>=1)?sets[0]:",:;=<>{}[]()";
		String escapeChars=(sets!=null && sets.length>=2)?sets[1]:null;
		String whiteChars=(sets!=null && sets.length>=3)?sets[2]:null;
		int escChar=-1;		// if not -1 then we are escaping
		char lastChar=0;
		char curChar=0;
		int lastOffset=offset;
		int isWhiteChar=-1;
		int isDelimChar=-1;
		boolean weakEscape=false;
		int controlCount=0; // counts number of  \\ to prevent shortcuit on even number
		while(offset<input.length()){ // only scan until the end
			lastChar=curChar;
			curChar=input.charAt(offset++); // from here on offset is ahead
			isWhiteChar=isElementOf(curChar,whiteChars);
			isDelimChar=isElementOf(curChar,delimChars);
			// determine if we should ignore testing for exit
			int isEscapeChar=isElementOf(curChar,escapeChars);
			controlCount=(lastChar=='\\')?controlCount+1:0; // control count counts number of \\ to
			if((controlCount%2)==1){
				isDelimChar=isEscapeChar=-1; // shortcircuit delimiting or escaping if prev char was \\ but only unevent number of times
			}
			if(escChar==-1){ // should we enter escaping
				if(isEscapeChar!=-1){
					// will enter escChar but only once
					escChar=isEscapeChar;
				} 
			}else{			 // should we exit escaping
				if(weakEscape==false) isDelimChar=-1; // shortcircuit delim signal if in escape mode and not weak
				// exit back to normal if escape found second time
				if(isEscapeChar==escChar){
					// special rule:if oldchar==curchar and next is not delim or whitespace we ignore escape char
					boolean isletter=offset<input.length() && !(isElementOf(input.charAt(offset),delimChars)!=-1 || isElementOf(input.charAt(offset),whiteChars)!=-1);
					if(lastChar==curChar && isletter){
						// we are special enter weak escaping (where delimiter is not ignored)
						// this will correct spurios double quotes but will recover forgotter delimiters between two parts
						// we stay in escape mode but listen for delims
						weakEscape=true; 
					}else{
						escChar=-1; // we are exiting escaping
					}
				}
			}
			// emit chars and test for exit
			if(escChar>=0 || isWhiteChar==-1 || isDelimChar!=-1){
				// emit if escaping or if delimiter or not white char
				out.append(curChar);
			} 
			if(isDelimChar!=-1) break; // exit delimiter found
		}
		if(isDelimChar!=-1 && lastOffset<(offset-1)){
			// fix end of out to not have a delimiter if it has any other string
			offset-=1;out.setLength(out.length()-1);
		}
		return offset;
	}
	/**Returns the next token and updated offset.
	 * An improved inline tokenizer using various rules to control delimiting, escaping and text swallowing.
	 * We supply an array of events or if none is provided a default delimiter event is constructed.
	 * After that the events are used to control tokenization. We enter a loop and feed the input to
	 * the events if one or more are armed or triggered (state >=0) we defer emiting chars to output until we determine what to do.
	 * For events that do escape we just defer until end of escape is detected, for delimit we return back and
	 * for supress we just swallow the input without emitting it.
	 */
	/*
	public static int nextToken(TokenizerRule state,int offset,CharSequence input, StringBuilder out){
		int emitCount=0;
		int oldOffset=offset;
		while(offset<input.length()){
			int st=state.consume(offset, input);
			st=(st==TokenizerRule.DO_DEFER && offset==(input.length()))?TokenizerRule.DO_EMIT:st;
			switch(st){
				case TokenizerRule.DO_EMIT:
					// we can emit what we got so far
					if(oldOffset<=offset){
						offset++;
						out.append(input,oldOffset,offset);
						emitCount+=(offset-oldOffset);
						oldOffset=offset;
					}				
					break;
				case TokenizerRule.DO_DEFER:
					// we need to defer emitting
					offset++;
					break;
				case TokenizerRule.DO_SKIP:
					// we just skip over this input
					offset++;
					oldOffset=offset; 
					break;
				case TokenizerRule.DO_EXITBEFORE:
					if(emitCount>0){
						offset-=(state.getSize()-1);
					}
				case TokenizerRule.DO_EXITAFTER:
					if(emitCount==0 && oldOffset<=offset){ 
						// if there is anything left 
						offset++;
						out.append(input,oldOffset,offset);
						emitCount+=(offset-oldOffset);
						oldOffset=offset;
					}
					state.clear();
				default:
					return st>=0?st:offset;
					
			}
		}
		return offset;
	}
	*/

}
