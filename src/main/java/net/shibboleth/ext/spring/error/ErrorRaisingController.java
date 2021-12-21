/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.ext.spring.error;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * MVC controller for responding to errors by dispatching them to the MVC error handling umbrella.
 */
@Controller
public class ErrorRaisingController {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ErrorRaisingController.class);

    /**
     * Handle an error dispatched by the container by re-raising it.
     * 
     * @param httpRequest the HTTP request
     * 
     * @throws Throwable if something goes wrong
     */
    @RequestMapping(value = "/RaiseError")
    // Checkstyle: IllegalThrows OFF
    public void raiseError(@Nonnull final HttpServletRequest httpRequest) throws Throwable {
        
        final Object uri = httpRequest.getAttribute("javax.servlet.error.request_uri");
        
        final Object exception = httpRequest.getAttribute("javax.servlet.error.exception");
        if (exception == null || !(exception instanceof Exception)) {
            log.error("No exception found in request attribute, raising a generic error");
            throw new IllegalArgumentException("No exception found in request attribute");
        }
        
        final Exception e = (Exception) exception;
        log.error("Propagating exception thrown by request to {}", uri);
        if (e.getCause() != null) {
            throw e.getCause();
        }
        
        throw e;
    }
    // Checkstyle: IllegalThrows ON
    
}