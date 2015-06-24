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

package net.shibboleth.ext.spring.factory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.net.URISupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring bean factory for producing a {@link String} containing a URL path constructed from
 * a base value with a query string appended based on specific parameters that may be found in
 * an {@link HttpServletRequest}. 
 */
public class QueryPassthroughFactoryBean implements FactoryBean<String> {

    /** Base location to build on. */
    @Nullable private String basePath;
    
    /** Parameters to pass through. */
    @Nonnull @NonnullElements private Collection<String> parameters;
    
    /** HTTP request to copy parameters from. */
    @Nullable private HttpServletRequest httpRequest;

    /** Constructor. */
    public QueryPassthroughFactoryBean() {
        parameters = Collections.emptyList();
    }
    
    /**
     * Set the base location to build on.
     *
     * @param path base location
     */
    public void setBasePath(@Nonnull final String path) {
        basePath = StringSupport.trimOrNull(path);
    }
    
    /**
     * Set the parameter names to look for in the request and pass through.
     * 
     * @param params parameter names
     */
    public void setParameters(@Nonnull @NonnullElements final Collection<String> params) {
        parameters = StringSupport.normalizeStringCollection(params);
    }

    /**
     * Set the resource containing the document to be parsed.
     *
     * @param request servlet request
     */
    public void setHttpServletRequest(@Nullable final HttpServletRequest request) {
        httpRequest = request;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    @Nonnull public synchronized String getObject() throws Exception {
        
        if (basePath == null) {
            throw new BeanCreationException("Base path cannot be null or empty.");
        }
        
        if (httpRequest == null || parameters.isEmpty()) {
            return basePath;
        }

        boolean firstParam = basePath.indexOf('?') == -1;
        
        final StringBuilder url = new StringBuilder(basePath);
        
        final Map<String,String[]> parameterMap = httpRequest.getParameterMap();
        for (final String paramName : parameters) {
            if (!parameterMap.containsKey(paramName)) {
                // The parameter to copy isn't in the original URL.
                continue;
            }

            final String[] values = parameterMap.get(paramName);
            if (values == null) {
                if (firstParam) {
                    firstParam = false;
                    url.append('?');
                } else {
                    url.append('&');
                }
                url.append(URISupport.doURLEncode(paramName)).append('=');
            }
            for (final String value : parameterMap.get(paramName)) {
                if (firstParam) {
                    firstParam = false;
                    url.append('?');
                } else {
                    url.append('&');
                }
                url.append(URISupport.doURLEncode(paramName)).append('=');
                if (value != null) {
                    url.append(URISupport.doURLEncode(value));
                }
            }
        }
        
        return url.toString();
    }
// Checkstyle: CyclomaticComplexity ON
    
    /** {@inheritDoc} */
    @Override
    @Nonnull public Class<?> getObjectType() {
        return String.class;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSingleton() {
        return true;
    }
}