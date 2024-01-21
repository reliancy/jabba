/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.util;
import java.util.HashMap;

/** One exception to rule them all.
 *  This exception works with ResultCode and represents and instance with context information.
 *  If a ResultCode is deemed parametric then we use provided parameters to update it when generating a message.
 * 
 * @author amer
 */
public class CodeException extends RuntimeException {

	protected final int code;
	protected final HashMap<String,Object> context=new HashMap<>();
	public CodeException(int code) {
		this.code = code;
	}
	public CodeException(Throwable cause, int code) {
		super(cause);
		this.code = code;
	}
	@Override
	public String toString(){
		return getMessage();
	}
	public int getCode() {
        return code;
    }
	public ResultCode getResultCode(){
		return ResultCode.get(code);
	}
	@Override
	public String getMessage() {
		ResultCode rcode=getResultCode();
		if(rcode!=null){
			boolean wrapped=(rcode.getCode()==ResultCode.FAILURE);
			String msg=rcode.getMessage();
			if(msg.contains("$")){
				for(String key:context.keySet()){
					Object obj=context.get(key);
					if(obj==null) continue;
					String val=String.valueOf(obj);
					msg=msg.replaceAll("\\$\\{"+key+"\\}",val);
				}
			}else if(this.getCause()!=null && wrapped){
				msg=CodeException.getUserMessage(this.getCause());
			}
			return msg;
		}else{
			return "("+String.format("%08X", code)+")";
		}
	}
	
    @SuppressWarnings("unchecked")
	public <T> T get(String name) {
        return (T)context.get(name);
    }
	
    public CodeException put(String name, String value) {
        context.put(name, value);
        return this;
    }
    public static CodeException wrap(Throwable exception) {
        if (exception instanceof CodeException) {
            CodeException se = (CodeException)exception;
			return se;
        } else {
            return new CodeException(exception,ResultCode.FAILURE);
        }
    }
	public static String getUserMessage(Throwable ex,Object context) {
		return getUserMessage(ex);
	}
	public static String getUserMessage(Throwable ex) {
		StringBuilder buf=new StringBuilder();
		fillUserMessage(ex,buf,null);
		return buf.toString();
	}
	public static Throwable fillUserMessage(Throwable ex,StringBuilder msg,StringBuilder title) {
		Throwable c = ex;
		//System.out.println(">>>"+c+"/"+c.getCause());
		while(c.getCause()!=null){
			Throwable cc= c.getCause();
			if(c.getMessage()==null){
				c=cc;continue;
			}
			String cMsg=c.getMessage();
			String ccMsg=cc.getMessage();
			//System.out.println("!!!"+cMsg+"/"+c.getClass().getName()+"/"+cc.getClass().getName());
			boolean wrapped=(c instanceof CodeException) && ((CodeException)c).getCode()==ResultCode.FAILURE;
			boolean plain_at=cMsg.equals(c.getClass().getName());
			boolean plain_sub=cMsg.equals(cc.getClass().getName());
			boolean same_msg=cMsg.equalsIgnoreCase(ccMsg);
			//System.out.println("\t"+plain_sub+"#"+cc+"$"+cc.getCause()+"*"+cc.getMessage());
			if(plain_at || plain_sub || cMsg.startsWith(cc.getClass().getName()+":") || same_msg || wrapped){
				c=cc;
			}else{
				break;
			}
		}
		//System.out.println("CC:"+c);
		// take care of title
		String _title=c.getClass().getSimpleName();
		if(c instanceof CodeException){
			CodeException cc=(CodeException) c;
			if(cc.getCause()!=null){
				_title=cc.getClass().getSimpleName();
			}else{
				// we do not have a cause
				int code=cc.getCode();
				ResultCode rcode=ResultCode.get(code);
				if(rcode!=null) _title=rcode.getSource();
			}
		}
		if(title!=null) title.append(_title);
		// now take care of detail
		String _msg=c.getLocalizedMessage();
		if(_msg==null || _msg.trim().isEmpty()){
			_msg=c.getClass().getSimpleName();
			StackTraceElement[] se=c.getStackTrace();
			if(se!=null && se.length>0) _msg+="\n\t at "+se[0].toString();
		}
		String prefString="Exception:";
		String prefString2="Error:";
		int prefix=_msg.lastIndexOf(prefString);
		if(prefix<0) prefix=_msg.lastIndexOf(prefString2);
		if(prefix>0 && _msg.substring(0, prefix).contains(".")) _msg=_msg.substring(prefix+prefString.length());
		if(msg!=null) msg.append(_msg);
		return c;
	}
}
