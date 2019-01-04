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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.io.Resource;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A wrapper that guards a {@link Resource} that may be absent by returning an empty bean file instead.
 * 
 * @since 5.4.0
 */
public class ConditionalResource extends AbstractIdentifiedInitializableComponent
        implements Resource, BeanNameAware, net.shibboleth.utilities.java.support.resource.Resource {

    /** Dummy content. */
    @Nonnull @NotEmpty private static final String EMPTY_RESOURCE =
            "<beans xmlns=\"http://www.springframework.org/schema/beans\""
                    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                    + " xsi:schemaLocation=\"http://www.springframework.org/schema/beans"
                    + " http://www.springframework.org/schema/beans/spring-beans.xsd\""
                    + "></beans>"; 
    
    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ConditionalResource.class);

    /** Cached log prefix. */
    @Nullable private String logPrefix;
    
    /** Resource to wrap. */
    @Nonnull private final Resource wrappedResource;
    
    /**
     * Constructor.
     *
     * @param wrapped the resource to wrap
     */
    public ConditionalResource(@Nonnull final Resource wrapped) {
        wrappedResource = Constraint.isNotNull(wrapped, "Wrapped resource cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override public void setId(@Nonnull @NotEmpty final String id) {
        super.setId(id);
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() throws IOException {
        try {
            return wrappedResource.getInputStream();
        } catch (final IOException e) {
            if (log.isTraceEnabled()) {
                log.trace("{} getInputStream failed on wrapped resource", getLogPrefix(), e);
            } else {
                log.debug("{} getInputStream failed on wrapped resource", getLogPrefix());
            }
            return new ByteArrayInputStream(EMPTY_RESOURCE.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** {@inheritDoc} */
    public net.shibboleth.utilities.java.support.resource.Resource createRelativeResource(final String relativePath)
            throws IOException {
        
        final Resource relative = wrappedResource.createRelative(relativePath);
        if (relative instanceof net.shibboleth.utilities.java.support.resource.Resource) {
            return (net.shibboleth.utilities.java.support.resource.Resource) relative;
        }
        
        return ResourceHelper.of(relative);
    }

    /** {@inheritDoc} */
    public void setBeanName(final String name) {
        setId(name);
    }

    /** {@inheritDoc} */
    public boolean exists() {
        if (!wrappedResource.exists()) {
            log.debug("{} Wrapped resource does not exist", getLogPrefix());
        }
        return true;
    }

    /** {@inheritDoc} */
    public boolean isReadable() {
        return true;
    }

    /** {@inheritDoc} */
    public boolean isOpen() {
        return wrappedResource.isOpen();
    }

    /** {@inheritDoc} */
    public URL getURL() throws IOException {
        try {
            return wrappedResource.getURL();
        } catch (final IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("{} getURL failed on wrapped resource", getLogPrefix(), e);
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    public URI getURI() throws IOException {
        try {
            return wrappedResource.getURI();
        } catch (final IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("{} getURI failed on wrapped resource", getLogPrefix(), e);
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    public File getFile() throws IOException {
        try {
            return wrappedResource.getFile();
        } catch (final IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("{} getFile failed on wrapped resource", getLogPrefix(), e);
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    public long contentLength() throws IOException {
        try {
            return wrappedResource.contentLength();
        } catch (final IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("{} contentLength failed on wrapped resource", getLogPrefix(), e);
            }
            return EMPTY_RESOURCE.length();
        }
    }

    /** {@inheritDoc} */
    public long lastModified() throws IOException {
        try {
            return wrappedResource.lastModified();
        } catch (final IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("{} lastModified failed on wrapped resource", getLogPrefix(), e);
            }
            return 0;
        }
    }

    /** {@inheritDoc} */
    public Resource createRelative(final String relativePath) throws IOException {
        return wrappedResource.createRelative(relativePath);
    }

    /** {@inheritDoc} */
    public String getFilename() {
        return wrappedResource.getFilename();
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return wrappedResource.getDescription();
    }

    /**
     * Return a prefix for logging messages for this component.
     * 
     * @return a string for insertion at the beginning of any log messages
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        if (logPrefix == null) {
            logPrefix = "ConditionalResource " + getId() + ":";
        }
        return logPrefix;
    }
    
}