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

import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.utilities.java.support.codec.HTMLEncoder;

import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

/**
 * Custom Spring exception to view mapper that populates the view model with data
 * obtained via an extension function.
 * 
 * <p>As a default, the view model will include a reference to the active {@link HttpServletRequest}
 * and the {@link HTMLEncoder} class.</p>
 */
public class ExtendedMappingExceptionResolver extends SimpleMappingExceptionResolver {

    /** Model attribute carrying {@link HttpServletRequest}. */
    @Nonnull private static final String MODEL_ATTR_REQUEST = "request";

    /** Model attribute carrying {@link WebApplicationContext}. */
    @Nonnull private static final String MODEL_ATTR_SPRINGCONTEXT = "springContext";

    /** Model attribute carrying the {@link HTMLEncoder} class. */
    @Nonnull private static final String MODEL_ATTR_ENCODER = "encoder";
    
    /** Function to obtain extensions to view model. */
    @Nullable private Function<HttpServletRequest,Map<String,Object>> viewModelExtenderFunction;
    
    /** Constructor. */
    public ExtendedMappingExceptionResolver() {
        
    }

    /**
     * Constructor.
     *
     * @param extender function to obtain extensions to view model
     */
    public ExtendedMappingExceptionResolver(@Nullable final Function<HttpServletRequest,Map<String,Object>> extender) {
        viewModelExtenderFunction = extender;
    }
    
    /** {@inheritDoc} */
    @Override protected ModelAndView getModelAndView(final String viewName,
            final Exception ex, final HttpServletRequest request) {
        
        LoggerFactory.getLogger(ex.getClass()).error("", ex);
        
        final ModelAndView view = super.getModelAndView(viewName, ex, request);
        view.addObject(MODEL_ATTR_REQUEST, request);
        view.addObject(MODEL_ATTR_ENCODER, HTMLEncoder.class);
        
        final WebApplicationContext context =
                WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
        if (context != null) {
            view.addObject(MODEL_ATTR_SPRINGCONTEXT, context);
        }
        
        if (viewModelExtenderFunction != null) {
            final Map<String,Object> exts = viewModelExtenderFunction.apply(request);
            if (exts != null) {
                view.addAllObjects(exts);
            }
        }
        
        return view;
    }

}