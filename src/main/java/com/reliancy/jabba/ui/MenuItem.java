/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba.ui;

import com.reliancy.util.Handy;

/** Individual menu items within a menu.
 * 
 */
public class MenuItem {
    final String id;
    String title;
    String url;
    String icon;
    public MenuItem(String id) {
        this.id=id;
        this.title=Handy.prettyPrint(id);
        this.url="/"+id;
    }
    public String getId() {
        return id;
    }
    public int getSize(){
        return 0;
    }
    public String getTitle() {
        return title;
    }
    public MenuItem setTitle(String title) {
        this.title = title;
        return this;
    }
    public String getUrl() {
        return url;
    }
    public MenuItem setUrl(String url) {
        this.url = url;
        return this;
    }
    public String getIcon() {
        return icon;
    }
    public MenuItem setIcon(String icon) {
        this.icon = icon;
        return this;
    }
    
}
