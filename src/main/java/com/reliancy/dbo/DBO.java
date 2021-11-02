package com.reliancy.dbo;


/** Instance of an entity, usually a row in a table.
 * 
 */
public class DBO{
    public static enum Status{
        NEW,USED,DELETED,COMPUTED
    }
    Terminal terminal;
    Entity type;
    Status status;
    public DBO() {
    }
    public Terminal getTerminal() {
        return terminal;
    }
    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }
    public Entity getType() {
        return type;
    }
    public void setType(Entity type) {
        this.type = type;
    }
    public Status getStatus(){
        return status;
    }
    public void setStatus(Status s) {
        this.status = s;
    }
}
