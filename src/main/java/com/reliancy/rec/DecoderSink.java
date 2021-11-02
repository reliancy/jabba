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
