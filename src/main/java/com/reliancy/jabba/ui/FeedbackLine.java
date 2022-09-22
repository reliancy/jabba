/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.ui;
/**
 * Message object to inform users about things.
 * 
 */
public class FeedbackLine {
    public static final String ERROR="danger";
    public static final String WARN="warning";
    public static final String INFO="info";
    String type;
    String message;
    public FeedbackLine(String typ,String message){
        this.type=typ;
        this.message=message;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public static FeedbackLine error(String message){
        return new FeedbackLine(ERROR, message);
    }
    public static FeedbackLine warn(String message){
        return new FeedbackLine(WARN, message);
    }
    public static FeedbackLine info(String message){
        return new FeedbackLine(INFO, message);
    }
}
