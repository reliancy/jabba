/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.util;

import java.util.HashMap;

/** Utility class to handle error codes and error messages.
 * Error codes are integers that describe outcome of an operation.
 * 
 * In cases when return codes are used and not exceptions thrown it is a mess to keep track of what they mean.
 * With this class we define a uniform way of managing return codes and allow for additional information.
 * This additional information can be text that could be localized and give better user information about what happened.
 * 
 * First we distinguish between success and failure. Any code that is negative is failure.
 * Default success code is 0 and provides no additional info. Any positive code is a warning or info and possibly carries extra meaning and 
 * or description.
 * 
 * success,failure,pending
 * source
 * parametric
 */
public class ResultCode {
	public static final byte TYPE_SUCCESS=0x0;
	public static final byte TYPE_PENDING=0x1;
	public static final byte TYPE_FAILURE=0xF;
	final int code;
	final String message;
	final String source;
	public ResultCode(byte type,short value,String src,String message) {
		this.message=message;
		this.source=src;
		this.code=ResultCode.getCode(type,value,source!=null?source.hashCode():0);
	}
	public int getCode() {
		return code;
	}

	public byte getType() {
		return getType(code);
	}

	public int getValue() {
		return getValue(code);
	}
	
	public String getSource() {
		return source;
	}
	public String getMessage() {
		return message;
	}
	@Override
	public String toString() {
		int code=getCode();
		String context=getSource();
		String message=getMessage();
		if(context!=null){
			return context+"("+String.format("%08X", code)+"):"+message;
		}else{
			return "("+String.format("%08X", code)+"):"+message;
		}
	}
	protected static final HashMap<Integer,ResultCode> codes=new HashMap<>();
	public static final int getCode(byte type,int value,int source){
		int st=(type  <<28)  & 0xF0000000;
		int sc=(source <<8)  & 0x0FFFFF00;
		int vl=(value)		 & 0x000000FF;
		return (int) (st | sc | vl);
	}
	public static final byte getType(int code){
		return (byte)((code>>28) & 0x0F);
	}
	public static final boolean testType(int code,byte st){
		return getType(code)==st;
	}
	public static final boolean isSuccess(int code){
		return testType(code,TYPE_SUCCESS);
	}
	public static final boolean isFailure(int code,int st){
		return testType(code,TYPE_FAILURE);
	}
	public static final boolean isPending(int code,int st){
		return testType(code,TYPE_PENDING);
	}
	public static final int getValue(int code){
		return (int)(code & 0x000000FF);
	}
	public static final int getSource(int code){
		return (int)((code & 0x0FFFFF00)>>8);
	}
	public static final synchronized ResultCode get(int code){
		if(codes==null) return null;
		return (ResultCode)codes.get(code);
	}
	public static final synchronized ResultCode put(ResultCode c){
		ResultCode old=(ResultCode) codes.get(c.getCode());
		codes.put(c.getCode(),c);
		return old;
	}
	public static final int define(byte type,int value,Class<?> source,String message){
		return define(type,value,source!=null?source.getSimpleName():null,message);
	}
	public static final int define(byte type,int value,String source,String message){
		int code=getCode(type,value,source!=null?source.hashCode():0);
		ResultCode c=get(code);
		if(c!=null){
			System.err.println("Result code redefinition(consider different value or source):"+c);
			return code;
		}
		c=new ResultCode(type, (short) value,source,message);
		put(c);
		return code;
	}
	public static final int defineSuccess(int value,Class<?>  source,String message){
		return define(TYPE_SUCCESS,value,source,message);
	}
	public static final int defineFailure(int value,Class<?>  source,String message){
		return define(TYPE_FAILURE,value,source,message);
	}
	public static final int definePending(int value,Class<?>  source,String message){
		return define(TYPE_PENDING,value,source,message);
	}
	public static final int SUCCESS=ResultCode.defineSuccess(0,null,"Success");
	public static final int FAILURE=ResultCode.defineFailure(0,null,"Failure");
	public static final int PENDING=ResultCode.definePending(0,null,"Pending");
}
