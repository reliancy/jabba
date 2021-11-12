package com.reliancy.dbo;

import java.io.Closeable;
import java.util.Iterator;
/** Iterator interface suitable for dbo resultsets.
 * 
 */
public interface SiphonIterator<T> extends Iterator<T>, Closeable {
}