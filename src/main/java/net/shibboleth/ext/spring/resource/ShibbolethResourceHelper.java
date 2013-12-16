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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resource.ShibbolethResource;

import org.springframework.core.io.Resource;

/**
 * Bridging class between {@link Resource} and {@link net.shibboleth.utilities.java.support.resource}.
 */
public final class ShibbolethResourceHelper implements ShibbolethResource {

    /** The cached Spring {@link Resource}. */
    private Resource springResource;

    /**
     * A private for shimming the provided input.
     * 
     * @param theResource the spring resource;
     */
    private ShibbolethResourceHelper(@Nonnull Resource theResource) {

        springResource = Constraint.isNotNull(theResource, "provided Spring Resource should not be null");
    }

    /**
     * Return a {@link ShibbolethResource} that does all the work of the provided {@link Resource}<br/>
     * If the input implements {@link ShibbolethResource} then it is cast to the output, other a shim class is
     * generated.
     * 
     * @param springResource the input
     * @return a {@link ShibbolethResource} which reflects what the Spring one does
     */
    public static ShibbolethResource of(Resource springResource) {
        if (springResource instanceof ShibbolethResource) {
            return (ShibbolethResource) springResource;
        }
        return new ShibbolethResourceHelper(springResource);
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() throws IOException {
        return springResource.getInputStream();
    }

    /** {@inheritDoc} */
    public boolean exists() {
        return springResource.exists();
    }

    /** {@inheritDoc} */
    public boolean isReadable() {
        return springResource.isReadable();
    }

    /** {@inheritDoc} */
    public boolean isOpen() {
        return springResource.isOpen();
    }

    /** {@inheritDoc} */
    public URL getURL() throws IOException {
        return springResource.getURL();
    }

    /** {@inheritDoc} */
    public URI getURI() throws IOException {
        return springResource.getURI();
    }

    /** {@inheritDoc} */
    public File getFile() throws IOException {
        return springResource.getFile();
    }

    /** {@inheritDoc} */
    public long contentLength() throws IOException {
        return springResource.contentLength();
    }

    /** {@inheritDoc} */
    public long lastModified() throws IOException {
        return springResource.lastModified();
    }

    /** {@inheritDoc} */
    public ShibbolethResource createRelative(String relativePath) throws IOException {
        
        return of(springResource.createRelative(relativePath));
    }

    /** {@inheritDoc} */
    public String getFilename() {
        return springResource.getFilename();
    }

    /** {@inheritDoc} */
    public String getDescription() {
        return springResource.getDescription();
    }

}
