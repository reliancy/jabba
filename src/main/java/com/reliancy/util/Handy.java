/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Common utility methods.
 */
public final class Handy {
	public static final String WHITE=" \t\r\f\n";
    
	/** place left-right around verb.
	 * @param verb body of text
	 * @param left to add left of verb
	 * @param left to add right of verb
	 * @return adjusted text
	 * */ 
	public static String wrap(String verb, String left, String right) {
        if(verb==null) verb="";
        if(verb.startsWith(left) && verb.endsWith(right)) return verb;
        return left+verb.trim()+right;
    }
    /** remove left-right around verb.
	 * @param verb body of text
	 * @param left to remove left of verb
	 * @param left to remove right of verb
	 * @return adjusted text
	 **/ 
    public static String unwrap(String verb, String left, String right) {
        if(verb==null) return verb;
        String ret=verb.trim();
        if(ret.startsWith(left) && ret.endsWith(right)){
			ret=ret.substring(left.length());
			ret=ret.substring(0,ret.length()-right.length());
		}
        return ret;
    }
	/** remove any chars elements from left of verb. 
	 * @param verb body of text
	 * @return adjusted text
	*/
	public static String trimLeft(String verb,String chars){
		while(verb.length()>0 && chars.indexOf(verb.charAt(0))!=-1){
			verb=verb.substring(1);
		}
		return verb;
	}
	/** remove any chars elements from right of verb. 
	 * @param verb body of text
	 * @return adjusted text
	*/
	public static String trimRight(String verb,String chars){
		while(verb.length()>0 && chars.indexOf(verb.charAt(verb.length()-1))!=-1){
			verb=verb.substring(0,verb.length()-1);
		}
		return verb;
	}
	/** remove any chars elements from right and right of verb.
	 * @param verb body of text
	 * @return adjusted text
	 */
	public static String trimBoth(String verb,String chars){
		verb=trimLeft(verb, chars);
		verb=trimRight(verb, chars);
		return verb;
	}
	/** remove any chars elements from right and right of verb symetrically. trims whitespace first. */
	public static String trimEvenly(String verb,String chars){
		verb=trimBoth(verb," \t\n\r\f");
		while(verb.length()>1){
			char left=verb.charAt(0);
			char right=verb.charAt(verb.length()-1);
			if(left!=right) break; // left-right not even
			if(chars.indexOf(left)<0) break; // even but not in chars list
			verb=verb.substring(1,verb.length()-1);
		}		
		return verb;
	}
    public static <T> T nz(T val, T def){
        return val!=null?val:def;
    }
    /** Convert incoming value to an expected class.
     * @param clazz expected class
     * @param val observed value
     * @return val converted to type clazz.
     */
    public static Object normalize(Class<?> clazz, Object val ) {
        if(val==null) return null; // we are null
        if(clazz.isAssignableFrom(val.getClass())) return val; // we are assignable
        if(val instanceof String){
            String value=(String) val;
            if(value.isEmpty() || value.equals("''") || value.equals("\"\"")) return null;
            if( Boolean.class==( clazz ) || boolean.class==( clazz ) ) return Boolean.parseBoolean( value );
            if( Byte.class==( clazz ) || byte.class==( clazz ) ) return Byte.parseByte( value );
            if( Short.class==( clazz ) || short.class==( clazz ) ) return Short.parseShort( value );
            if( Integer.class==( clazz ) || int.class==( clazz ) ) return Integer.parseInt( value );
            if( Long.class==( clazz ) || long.class==( clazz )) return Long.parseLong( value );
            if( Float.class==( clazz ) || float.class==( clazz ) ) return Float.parseFloat( value );
            if( Double.class==( clazz ) || double.class==( clazz )) return Double.parseDouble( value );
        }
		if(clazz==String.class || clazz==CharSequence.class){
			return String.valueOf(val);
		}
        return val;
    } 
    /**
	 * This method is a bit more complex because it locks onto two delimiters one for grouping and other 
	 * for decimal point and chooses those from a list of [space],'`. which are used all over the world in different places.
     * Returns true if the string only contains digits and numeric characters.
	 * This should match 1000000 also 1,000,000 and also 1,000,000.00 but it is still not possible to differentiate between 1,000 =1000 in us
	 * from 1000,00 whch is used in europe. So it is difficult to normalize the string so it could process any number.
	 * @param str string to test
	 * @return trie if string looks numeric or is null/empty
     */
    public static final boolean isNumeric(String str){
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        String delims=" ,.'`";
        int delimNumber=0; // 0 we load, 1 we let one more, 2 we exit on second occurance
        char delimUsed=0; // char used as delim
        boolean delimLast=false;
        int digitCount=0;
        for (int i = 0; i < strLen; i++) {
            char ch=str.charAt(i);
            boolean accept=Character.isDigit(ch);
            if(accept) digitCount++;
            accept=accept || (ch=='-' && i==0);
            accept=accept || (ch=='+' && i==0);
            if(delims.indexOf(ch)>=0){
                accept=!delimLast; // prevent delims following each other
                delimLast=true;
                if(delimNumber==0){
                    delimNumber=1;
                    delimUsed=ch;
                }else if(delimNumber==1){
                    if(delimUsed!=ch) delimNumber=2;
                    delimUsed=ch;
                }else{
                    // we have seen two different delim and whatever is coming here is breaking numeric format like second delim second time or some otehr
                    accept=false;
                }
            }else{
                delimLast=false;
            }
            if(!accept) return false;
        }
        return digitCount>0;
    }
	/**
	 * @return true if the string is null, empty or contains only white space.
	 */
    public static boolean isBlank(CharSequence str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }
    /**
     * Provides a unified notion of what constitutes an empty value.
     * For any object if it is null.
     * For string also if it is blank
     * For arrays and lists and collection and maps also if no entries or keys exist.
     * @param value anything
     * @return true if any of the above matches
     */
    public static boolean isEmpty(Object value){
        if(value==null) return true;
        if(value instanceof CharSequence){
            return isBlank((CharSequence)value);
        }
        Class<?> cls=value.getClass();
        if(cls.isArray()) {
			if(value instanceof Object[]){
				Object[] arr=(Object[]) value;
				if(arr.length==0) return true;
				for(int i=0;i<arr.length;i++) if(isEmpty(arr[i])==false) return false;
				return true;
			}if(value instanceof byte[]){
				return ((byte[])value).length==0;
			}else if(value instanceof short[]){
				return ((short[])value).length==0;
			}else if(value instanceof int[]){
				return ((int[])value).length==0;
			}else if(value instanceof long[]){
				return ((long[])value).length==0;
			}else if(value instanceof float[]){
				return ((float[])value).length==0;
			}else if(value instanceof double[]){
				return ((double[])value).length==0;
			}else{
				return false;
			}
        }
        if(value instanceof Collection){
			Collection<?> c=(Collection<?>)value;
			if(c.isEmpty()) return true;
			for(Object o:c){
				if(isEmpty(o)==false) return false;
			}
			return true;
		}
        return false;
    }
    /** Attempts to take a compact string and beautify it.
	 * Will uppercase the first letter. Will also expand CamelCase.
	 * Also will replace _ with empty space.
	 * @param str
	 * @return nicely formatted string ready for display
	 */
	public static final String prettyPrint(String str){
		if(str==null) return "";
		boolean fix=false;
		char prevCh=0;
		if(str.startsWith("org.") || str.startsWith("net.") || str.startsWith("com.") || str.startsWith("java.")){
			str=str.substring(1+str.lastIndexOf('.')); // we strip class name paths
		}
		for(int i=0;i<str.length();i++){
			char currCh=str.charAt(i);
			if(i==0 && Character.isLowerCase(currCh)) fix=true;
			if(Character.isLowerCase(prevCh) && Character.isUpperCase(currCh)) fix=true;
			if(Character.isUpperCase(prevCh) && Character.isUpperCase(currCh) && i<(str.length()-1) && Character.isLowerCase(str.charAt(i+1))) fix=true;
			if(!Character.isLetter(currCh)) fix=true;
			prevCh=currCh;
		}
		if(!fix) return str;
		StringBuilder bufs=new StringBuilder();
		boolean toUC=false;
		for(int i=0;i<str.length();i++){
			char currCh=str.charAt(i);
			if(currCh=='_') currCh=' ';
			prevCh=bufs.length()>0?bufs.charAt(bufs.length()-1):currCh;
			if(Character.isWhitespace(currCh)){
				 if(!Character.isWhitespace(prevCh)){
					 bufs.append(' ');
				 }
				 continue; // ignore repeated whitespace otherwise emit space
			}
			if(bufs.length()==0){
				toUC=true;
			}else if((!Character.isUpperCase(prevCh) && ("-+/%*".indexOf(prevCh)==-1 || Character.isLetter(prevCh))) && Character.isUpperCase(currCh)){
				// non uc (a not one of operands) behind, uc ahead
				bufs.append(" ");
			}else if(Character.isLetter(prevCh) && Character.isDigit(currCh)){
				// letter behind, digit ahead
				bufs.append(" ");
			}else if(Character.isUpperCase(prevCh) && Character.isUpperCase(currCh) && i<(str.length()-1) && Character.isLowerCase(str.charAt(i+1))){
				// behind me uppercase infrom uppercase then lowercase
				bufs.append(" ");
			}
			bufs.append(toUC?Character.toUpperCase(currCh):currCh);
			toUC=false;
		}
		while(bufs.length()>0 && Character.isWhitespace(bufs.charAt(bufs.length()-1))){
			// trims whitespace from end
			bufs.setLength(bufs.length()-1);
		}
		return bufs.toString();
    }
    /** Attempts to take a user string and compact it to camel case.
	 * @param value more or less presentable string
	 * @return nicely compact string
	 */
	public static String toCamelCase(String value) {
		if(value==null || value.trim().isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
	    //final char delimChar = ' ';
		boolean flip = false;
		for (int charInd = 0; charInd < value.length(); charInd++) {
			char ch = value.charAt(charInd);
			if (Character.isWhitespace(ch)) {
				flip = true;
			}else if(flip){
				flip = false;
				if(ch==Character.toLowerCase(ch)) sb.append("_");
				sb.append(ch);
			}else{
				sb.append(ch);
			}
		}
	    return sb.toString();
  }
	public static byte[] deflate(byte[] content) throws IOException{
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION,true);  
		deflater.setInput(content);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(content.length);   
		deflater.finish();  
		byte[] buffer = new byte[1024];   
		while (!deflater.finished()) {  
			int count = deflater.deflate(buffer); // returns the generated code... index  
			outputStream.write(buffer, 0, count);   
		}  
		outputStream.close();  
		byte[] output = outputStream.toByteArray();  
	    return output;      
	}

    public static byte[] inflate(byte[] contentBytes) throws IOException, DataFormatException{
		Inflater inflater = new Inflater(true);   
		inflater.setInput(contentBytes);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(contentBytes.length);  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
			int count = inflater.inflate(buffer);  
			outputStream.write(buffer, 0, count);  
		}  
		outputStream.close();  
		byte[] output = outputStream.toByteArray();  
		return output;      
	}
	
	public static void shuffle(Object[] e){
		Random rn = new Random();
		for(int i=0;i<e.length;i++){
			int other=rn.nextInt(e.length);
			Object tmp=e[i];
			e[i]=e[other];
			e[other]=tmp;
		}
	}
	public static String encodeBase64(byte[] data){
		return Base64.getEncoder().encodeToString(data);
	}
	public static byte[] decodeBase64(String data){
		return Base64.getDecoder().decode(data);
	}
	/** Simple XOR encryption of a map of key-value pairs.
	 *  We randomize the order of key value pairs to make the string more unpredictable.
	 * Returned string is base64 and web safe
	 * @param key encryption key
	 * @param m map of param-value pairs to encrypt values.
	 * @return a string of encoded map param-value pairs which were then encrypted
	 */
	public static final String encrypt(String key,Map<String,String> m){
		String ret=null;
		Object[] es=m.keySet().toArray();
		shuffle(es); // we shuffle entries to confuse the string a bit
		StringBuilder buf=new StringBuilder();
		for(int i=0;i<es.length;i++){
			Object e=es[i];
			if(i>0) buf.append("\n");
			buf.append(e).append(":").append(m.get(e));
		}
		ret=encryptString(key,buf.toString());
		return ret;
	}
	/**
	 * This method will encrypt a string and return BASE 64 string that is web safe.
	 * TO make the string web safe we replace + with - and / with _
	 * Must revert this change on the reverse.
	 * @param key
	 * @param ret
	 * @return 
	 */
	public static final String encryptString(String key,String ret){
		try{
			byte[] bkey=key.getBytes("UTF-8");
			byte[] bstr=ret.getBytes("UTF-8");
			for(int i=0;i<bstr.length;i++){
				bstr[i]=(byte)(bstr[i] ^ bkey[i%bkey.length]);
			}
			// now need to encode this
			ret=encodeBase64(bstr);
			ret=ret.replace('+','-');
			ret=ret.replace('/','_');
			ret=ret.replace('=','.');
			ret=ret.replace("\n","");
		}catch(Exception e){
			ret="";
		}
		return ret;
	}
	/**Reverses the effects of encrypt.
	 * Also changes 
	 * @param key
	 * @param m
	 * @return values decrypted and parsed into key-value pair along newline.
	 */
	public static final Map<String,String> decrypt(String key,String m){
		m=decryptString(key,m);	
		Map<String,String> ret=new HashMap<>();
		//System.out.println("Output:"+m);
		Tokenizer tokz=new Tokenizer(m);
		tokz.setDelimChars("\n");
		tokz.setWhiteChars(null);
		for(String t=tokz.nextToken();t!=null;t=tokz.nextToken()){
			if("\n".equals(t)) continue;
			String[] kv=t.split(":",2);
			ret.put(kv[0],kv.length>1?kv[1]:null);
		}
		return ret;
	}
	public static final String decryptString(String key,String m){
		try{
			//m=URLDecoder.decode(m, "UTF-8");
			m=m.replace('-','+');
			m=m.replace('_','/');
			m=m.replace('.','=');
			byte[] bkey=key.getBytes("UTF-8");
			byte[] bstr=decodeBase64(m);
			for(int i=0;i<bstr.length;i++){
				bstr[i]=(byte)(bstr[i] ^ bkey[i%bkey.length]);
			}
			m=new String(bstr,"UTF-8");
		}catch(Exception e){
		}
		return m;
	}
	/**
	 * Generates a hash string with the algorithm name prefixed.
	 * @param message text to hash
	 * @param algorithm algorithm to use
	 * @return hash digest
	 */
	public static String hashString(String message, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException{
		if(message==null) return message;
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
		return algorithm.toLowerCase()+":"+encodeBase64(hashedBytes);
    }	
	/** hash text using sha256. */
	public static String hashSHA256(String message){
		try{
			return hashString(message,"SHA-256");
		}catch(Exception ex){
			return "sha-256:"+Integer.toHexString(message.hashCode());
		}
	}
	public static String hashMD5(String input){
		try { 
            MessageDigest md = MessageDigest.getInstance("MD5"); 
            byte[] messageDigest = md.digest(input.getBytes()); 
            BigInteger no = new BigInteger(1, messageDigest); 
            String hashtext = no.toString(16); 
            while (hashtext.length() < 32) { 
                hashtext = "0" + hashtext; 
            } 
            return hashtext; 
        }catch (NoSuchAlgorithmException e) {  // For specifying wrong message digest algorithms 
            throw new RuntimeException(e); 
        } 		
	}
	public static String toHexString(byte[] hash){
		char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
		StringBuilder sb = new StringBuilder(hash.length * 2);
		for (byte b : hash) {
			sb.append(HEX_CHARS[(b & 0xF0) >> 4]);
			sb.append(HEX_CHARS[b & 0x0F]);
		}
		return sb.toString();		
	}
	/**
	 * Finds first occurrence of sub inside body with and without case.
	 * We implement this search via a FSM and ignore the case.
	 * @param body text to search
	 * @param sub subsequence to find
	 * @param offset offset from 0
	 * @return offset of next occurance starting at offiset
	 */
	public static final int indexOf(CharSequence body,CharSequence sub,int offset){
		if(body==null) return -1;
		int state=0;
		int blen=body.length();
		int slen=sub.length();
		boolean ignorecase=true;
		for(int index=offset;index<blen;index++){
			char bC=body.charAt(index);
			char sC=sub.charAt(state);
			if(ignorecase){
				bC=Character.toLowerCase(bC);
				sC=Character.toLowerCase(sC);
			}
			if(bC==sC) state+=1; else state=0;
			if(state>=slen) return index-slen+1; // we found a match
		}
		return -1;
	}
	/**
	 * Will trim the string from left and right and remove any of the symbols.
	 * @param trim text to strip
	 * @param sym set of characters to trim
	 */
	public static String trim(String trim,String sym) {
		if(trim==null || trim.length()==0) return trim;
		int start=0;
		int end=trim.length();
		while(start<trim.length() && sym.indexOf(trim.charAt(start))!=-1) start++;
		while(0<end && sym.indexOf(trim.charAt(end-1))!=-1) end--;
		if(start==0 && end==trim.length()) return trim;
		return start<end?trim.substring(start, end):"";
	}
	/** will copy contents of a list into a fixed length array */
	public static String[] asArray(List<String> all) {
		if(all==null) return null;
		String[] ret=new String[all.size()];
		all.toArray(ret);
		return ret;
	}
	public static String toString(Object...args){
		StringBuilder buf=new StringBuilder();
		if(args.length>1){
			buf.append("[");
			for(int i=0;i<args.length;i++) buf.append(i==0?"":",").append(toString(args[i]));
			buf.append("]");
		}else if(args.length==1){
			Object arg=args[0];
			if(arg instanceof Iterable){
				java.util.Iterator<?> it=((Iterable<?>)arg).iterator();
				buf.append("[");
				while(it.hasNext()) buf.append(buf.length()>1?",":"").append(it.next());
				buf.append("]");
			}else
			if(arg instanceof java.util.Map){
				java.util.Map<?,?> marg=(Map<?,?>) arg;
				buf.append("{");
				for(java.util.Map.Entry<?,?>e:marg.entrySet()){
					buf.append(e.getKey().toString()).append(":").append(toString(e.getValue()));
				}
				buf.append("}");
			}else{
				buf.append(String.valueOf(arg));
			}
		}
		return buf.toString();
	}
    /** splitting without using regex.
	 * leading or trailing delims will produce empty tokens.
	 * if you need to split on a set of single chars please use tokenizer.
	 * @param delim delim string
	 * @param str body of text to chop
	 * @param delim_count maximal number of splits or -1 for all
	 * @return array of tokens
	*/
	public static String[] split(String delim,String str,int delim_count) {
		ArrayList<String> ret=new ArrayList<>();
		int delimLen=delim!=null?delim.length():0;
		int len=str.length();
		int index=0;
		int delimCnt=0; 		// track splits
		int delimAt=0;		// last delim position
		while(index<len){
			if(delim_count>0 && delimCnt>=delim_count) break; // reached limit of delims
			delimAt=delimLen>0?str.indexOf(delim, index):index+1;
			if(delimAt<0) break; // no more delims
			// we got a hit
			delimCnt+=1;
			ret.add(str.substring(index, delimAt)); // add token
			index=delimAt+delimLen;
		}
		if(index<len || delimAt>0){
			// add remainder (no more delim or delim at end)
			ret.add(str.substring(index));
		}
		return ret.toArray(new String[ret.size()]);
    }
    /** split a string as often as possible. */
	public static String[] split(String delim,String str) {
		return split(delim,str,-1);
    }
	@SafeVarargs
	public static <T> Iterator<T> chainIterators(Iterator<T>...its){
		return new JointIterator<T>(its);
	}
}
