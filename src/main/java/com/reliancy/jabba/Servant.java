/* 
Copyright (c) 2011-2022 Reliancy LLC

Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3.
You may obtain a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html.
You may not use this file except in compliance with the License. 
*/
package com.reliancy.jabba;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for objects that can serve responses for requests.
 */
public interface Servant {
    /**
     * Process a request and generate a response.
     * @param request the request to process
     * @param response the response to populate
     * @throws IOException if processing fails
     */
    void serve(Request request, Response response) throws IOException;
}

