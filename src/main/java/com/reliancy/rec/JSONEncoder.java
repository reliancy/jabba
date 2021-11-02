package com.reliancy.rec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JSONEncoder{
    public JSONEncoder(){
    }
    /**
	 * We encode into an appendable various primitives and Rec.
     * If appendable null then we just compute expected size.
	 * keys are not escaped they better not contain any special chars.
	 * values are quoted and escaped unless we detect a string that looks like a json object those are passed thru.
	 * in the past we tried to deduce if quoting was needed, but this is not the place to do so because we do not know how many times
	 * value was escaped so the only thing we can assume is that it needs to be escaped. So feeding a value that is quoted and 
	 * escaped will return back on parse the same and will need to dequoted and descaped once more but that shoudl work fine with
	 * whoever quoted it in the upstream in the first place.
	 * @param val property value
	 * @param o encoding output
	 * @return length in characters of encoded result
	 * @throws IOException 
	 */
	public static int encode(Object val,Appendable o) throws IOException {
		int len = 0;
		/*
        // first key
		if (key != null) {
			if (o != null) {
				o.append('"').append(key).append("\":");
			}
			len += 3 + key.length();
		}
        */
		// now value
		if (val instanceof Object[]) {
			Object[] valval = (Object[]) val;
			if (o != null) {
				o.append('[');
			}
			int index = 0;
			for (Object obj : valval) {
				if (index++ > 0) {
					len += 1;
					if (o != null) {
						o.append(",");
					}
				}
				len += encode(obj, o);
			}
			if (o != null) {
				o.append(']');
			}
			len += 2;
		} else if (val instanceof List) {
			List<?> valval = (List<?>) val;
			if (o != null) {
				o.append('[');
			}
			int index = 0;
			for (Object obj : valval) {
				if (index++ > 0) {
					len += 1;
					if (o != null) {
						o.append(",");
					}
				}
				len += encode(obj, o);
			}
			if (o != null) {
				o.append(']');
			}
			len += 2;
		} else if (val instanceof Map) {
            len+=encodeMap((Map<?,?>)val,o);
		} else if (val instanceof Rec) {
			len += encodeRec((Rec) val, o);
		} else if (val instanceof Number || val instanceof Boolean) {
			String str = val.toString();
			if (o != null) {
				o.append(str);
			}
			len += str.length();
		}else if(val instanceof int[]){
			int[] valval = (int[]) val;
			if (o != null) {
				o.append('[');
			}
			int index = 0;
			for (int obj : valval) {
				if (index++ > 0) {
					len += 1;
					if (o != null) {
						o.append(",");
					}
				}
				if(o!=null) o.append(String.valueOf(obj));
				len += 1;
			}
			if (o != null) {
				o.append(']');
			}
			len += 2;
		}else  if(val instanceof float[]){
			float[] valval = (float[]) val;
			if (o != null) {
				o.append('[');
			}
			int index = 0;
			for (float obj : valval) {
				if (index++ > 0) {
					len += 1;
					if (o != null) {
						o.append(",");
					}
				}
				if(o!=null) o.append(String.valueOf(obj));
				len += 1;
			}
			if (o != null) {
				o.append(']');
			}
			len += 2;
		}else if (val instanceof Object) {
			String str = val.toString();
			boolean jsontxt = false;
			jsontxt |= str.length() > 0 && str.startsWith("{") && str.endsWith("}");
			jsontxt |= str.length() > 0 && str.startsWith("[") && str.endsWith("]");
			//boolean quoted=str.length() > 1 && str.startsWith("\"") && str.endsWith("\"");
			// embedded json is not quoted and not escaped
			// all other text is quoted otherwise we will prevent quoted quotes (those would be swallowed)
			// we will not try to be smart if someone added an item that is quoted already it will be escaped and queotes retained
			// we must be consistent so that repeated parse and encode works and not too smart here
			// we need to put quotes around unless
			if (!jsontxt) {
				str = escape(str);
				if (o != null) {
					o.append('"');
				}
				len += 1;
			}
			if (o != null) {
				o.append(str);
			}
			len += str.length();
			if (!jsontxt) {
				if (o != null) {
					o.append('"');
				}
				len += 1;
			}
		} else if (val == null) {
			String str = "null";
			if (o != null) {
				o.append(str);
			}
			len += str.length();
		}
		return len;
	}
    public static int encodeMap(Map<?,?> valval,Appendable o) throws IOException{
        int len=0;
        if (o != null) {
            o.append('{');
        }
        int index = 0;
        for (Object obj : valval.keySet()) {
            if (index++ > 0) {
                len += 1;
                if (o != null) {
                    o.append(",");
                }
            }
            String key=obj.toString();
            if (o != null) {
                o.append('"').append(key).append("\":");
            }
            len += 3 + key.length();
            len += encode(valval.get(obj), o);
        }
        if (o != null) {
            o.append('}');
        }
        len += 2;
        return len;
    }
    public static int encodeRec(Rec val,Appendable o) throws IOException{
        int len=0;
        if (o != null) {
            o.append(val.isArray()?"[":"{");
        }
        for (int i=0;i<val.count();i++) {
            Slot k=val.getSlot(i);
            Object v=val.get(i);
            if (i > 0) {
                len += 1;
                if (o != null) {
                    o.append(",");
                }
            }
            if(k!=null){
                String key=k.getName();
                if (o != null) {
                    o.append('"').append(key).append("\":");
                }
                len += 3 + key.length();
            }
            len += encode(v, o);
        }
        if (o != null) {
            o.append(val.isArray()?"]":"}");
        }
        len += 2;
        return len;
    }
	/**
	 * @param str
	 * @return true if the string includes any of the special chars.
	 */
	public static boolean needsEscaping(String str) {
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			switch (ch) {
				case '"':
				case '\\':
				case '/':
				case '\b':
				case '\f':
				case '\n':
				case '\r':
				case '\t':
					return true;
			}
		}
		return false;
	}

	/**
	 * this helper method handle quotes and control chars.
	 * @param str input string
	 * @return output after encoding special chars
	 */
	public static String escape(String str) {
		StringBuilder buf = null;
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			switch (ch) {
				case '"':
					if(buf==null) buf=new StringBuilder(str.substring(0,i));
					buf.append("\\\"");
					break;
				case '\\':
					if(buf==null) buf=new StringBuilder(str.substring(0,i));
					buf.append("\\\\");
					break;
				case '/':
					if(buf==null) buf=new StringBuilder(str.substring(0,i));
					buf.append("\\/");
					break;
				case '\b':
					if(buf==null) buf=new StringBuilder(str.substring(0,i));
					buf.append("\\b");
					break;
				case '\f':
					if(buf==null) buf=new StringBuilder(str.substring(0,i));
					buf.append("\\f");
					break;
				case '\n':
					if(buf==null) buf=new StringBuilder(str.substring(0,i));
					buf.append("\\n");
					break;
				case '\r':
					if(buf==null) buf=new StringBuilder(str.substring(0,i));
					buf.append("\\r");
					break;
				case '\t':
					if(buf==null) buf=new StringBuilder(str.substring(0,i));
					buf.append("\\t");
					break;
				default:
					if(buf!=null) buf.append(ch);
			}
		}
		return buf!=null?buf.toString():str;
	}

}
