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

package net.shibboleth.ext.spring.resource;

import javax.annotation.Nonnull;

import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Supports a "conditional:resourcepath" URL syntax for wrapping a Spring resource with a
 * {@link ConditionalResource}.
 */
public class ConditionalResourceResolver implements ProtocolResolver {

    /** URL scheme. */
    @Nonnull @NotEmpty static final String CONDITIONAL_RESOURCE_PREFIX = "conditional:";
    
    /**
     * Constructor.
     */
    public ConditionalResourceResolver() {
        
    }

    /** {@inheritDoc} */
    public Resource resolve(final String location, final ResourceLoader resourceLoader) {
        
        if (location.startsWith(CONDITIONAL_RESOURCE_PREFIX)) {
            final Resource wrapped = resourceLoader.getResource(
                    location.substring(CONDITIONAL_RESOURCE_PREFIX.length()));
            if (wrapped != null) {
                final ConditionalResource r = new ConditionalResource(wrapped);
                r.setId(location);
                try {
                    r.initialize();
                } catch (final ComponentInitializationException e) {
                    // This doesn't happen.
                }
                
                return r;
            }
        }
        
        return null;
    }

}