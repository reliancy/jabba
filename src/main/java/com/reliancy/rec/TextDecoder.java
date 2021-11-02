/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.reliancy.rec;

/** An interface used in parser implementation. 
 * 
 * @author amer
 */
public interface TextDecoder {
	void beginDocument(Rec init);
	Rec endDocument();
	public int parse(int offset,CharSequence in);
}
