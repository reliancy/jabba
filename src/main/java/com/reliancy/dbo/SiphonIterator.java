/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.dbo;

import java.io.Closeable;
import java.util.Iterator;
/** Iterator interface suitable for dbo resultsets.
 * 
 */
public interface SiphonIterator<T> extends Iterator<T>, Closeable {
}