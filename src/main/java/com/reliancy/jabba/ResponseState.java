/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

/**
 * Represents the lifecycle state of a Response object.
 * Response goes through stages: created -&gt; configuring -&gt; committed -&gt; writing &lt;-&gt; written -&gt; completed
 */
public enum ResponseState {
    /** Response object created, nothing configured yet */
    CREATED,
    /** Headers, status, content type can be set */
    CONFIGURING,
    /** Response committed to HttpServletResponse (headers locked) */
    COMMITTED,
    /** Body content is being written */
    WRITING,
    /** Body content has been written */
    WRITTEN,
    /** Response fully done */
    COMPLETED;
    
    /**
     * Check if state allows setting headers/status/content type.
     * @return true if headers can be modified
     */
    public boolean canConfigure() {
        return this == CREATED || this == CONFIGURING;
    }
    
    /**
     * Check if state allows writing body content.
     * @return true if body can be written
     */
    public boolean canWrite() {
        return this == COMMITTED || this == WRITING || this == WRITTEN;
    }
    
    /**
     * Check if state allows flushing.
     * @return true if response can be flushed
     */
    public boolean canFlush() {
        return this == WRITING || this == WRITTEN || this == COMMITTED;
    }
    
    /**
     * Check if response has been written (body content exists).
     * @return true if body has been written
     */
    public boolean isWritten() {
        return this == WRITTEN || this == COMPLETED;
    }
    
    /**
     * Check if response is committed (headers locked).
     * @return true if response is committed
     */
    public boolean isCommitted() {
        return this == COMMITTED || this == WRITING || this == WRITTEN || this == COMPLETED;
    }
    
    /**
     * Check if response is completed (fully done).
     * @return true if response is completed
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
    
    /**
     * Transition from this state to a new state. Validates the transition.
     * Automatically handles intermediate state transitions (e.g., CREATED -> CONFIGURING when configuring,
     * CONFIGURING -> WRITING when writing).
     * @param to the target state
     * @return the new state if transition is valid
     * @throws IllegalStateException if transition is invalid
     */
    public ResponseState transitionTo(ResponseState to) {
        if(this == to) return this;
        // Auto-transition to intermediate states if needed
        if(to == CONFIGURING) {
            // Allow transition to CONFIGURING from CREATED
            if(this == CREATED) {
                return CONFIGURING;
            }
            // If already in CONFIGURING or later, check if we can still configure
            if(!this.canConfigure()) {
                throw new IllegalStateException("Cannot configure in state: " + this);
            }
        } else if(to == COMMITTED) {
            // Allow transition to COMMITTED from CONFIGURING
            if(this == CONFIGURING) {
                return COMMITTED;
            }
            // If already committed or later, stay in current state
            if(this == COMMITTED || this == WRITING || this == WRITTEN || this == COMPLETED) {
                return this;
            }
        } else if(to == WRITING) {
            // Allow transition to WRITING from COMMITTED, WRITING, or WRITTEN
            if(this == COMMITTED) {
                return WRITING;
            }
            if(this == WRITTEN) {
                return WRITING; // Can go back to writing to append more content
            }
            // Check if we can write
            if(!this.canWrite()) {
                throw new IllegalStateException("Cannot write in state: " + this);
            }
        }
        
        // Validate direct state transitions
        switch(this) {
            case CREATED:
                if(to != CONFIGURING && to != COMPLETED) {
                    throw new IllegalStateException("Invalid transition from CREATED to " + to);
                }
                break;
            case CONFIGURING:
                if(to != COMMITTED && to != WRITING && to != COMPLETED) {
                    throw new IllegalStateException("Invalid transition from CONFIGURING to " + to);
                }
                break;
            case WRITING:
                if(to != WRITTEN && to != COMPLETED) {
                    throw new IllegalStateException("Invalid transition from WRITING to " + to);
                }
                break;
            case WRITTEN:
                if(to != WRITING && to != COMPLETED) {
                    throw new IllegalStateException("Invalid transition from WRITTEN to " + to);
                }
                break;
            case COMMITTED:
                if(to != WRITING && to != COMPLETED) {
                    throw new IllegalStateException("Invalid transition from COMMITTED to " + to);
                }
                break;
            case COMPLETED:
                throw new IllegalStateException("Cannot transition from COMPLETED state");
        }
        return to;
    }
}

