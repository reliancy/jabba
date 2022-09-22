/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/

package com.reliancy.rec;

/** Similar to a SAX interface used by parsers for XML and JSON to assemble DOM structures.
 * Simply gets notified of events during parsing.
 * @author amer
 */
public interface DecoderSink {
	void beginDocument(Rec init);
	Rec endDocument();
	void beginElement(String name);
	void endElement(String name);
	void setKey(String name);
	void setValue(CharSequence seq);
}
