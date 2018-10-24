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

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.io.FileSystemResource;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resource.Resource;

/**
 * A file backed resource which calls out to a provided {@link Runnable} as appropriate such that the resource becomes
 * reloadable.
 */
public class RunnableFileSystemResource extends FileSystemResource
        implements Resource, org.springframework.core.io.Resource {

    /** What to run at the appropriate time. */
    @Nonnull private final Runnable theRunnable;

    /** The log. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RunnableFileSystemResource.class);

    /** Log prefix. */
    @Nonnull @NotEmpty private final String thePrefix;

    /**
     * Constructor.
     *
     * @param file The file to back.
     * @param runnable a {@link Runnable} to call at appropriate times
     */
    public RunnableFileSystemResource(@Nonnull @ParameterName(name = "file") final File file,
            @Nonnull @ParameterName(name = "runnable") final Runnable runnable) {
        super(Constraint.isNotNull(file, "File parameter to RunnableFileSystemResource cannot be null"));
        theRunnable = Constraint.isNotNull(runnable, "Runnable parameter to RunnableFileSystemResource cannot be null");
        thePrefix = "RunnableResource [" + getPath() + "]";

        try {
            callRunnable();
        } catch (final IOException ex) {
            throw new BeanCreationException(ex.getMessage());
        }
    }

    /**
     * Constructor.
     *
     * @param path the path to the file to look at.
     * @param runnable a {@link Runnable} to call at appropriate times
     */
    public RunnableFileSystemResource(@Nonnull @NotEmpty @ParameterName(name = "path") final String path,
            @Nonnull @ParameterName(name = "runnable") final Runnable runnable) {
        super(Constraint.isNotEmpty(path, "Path parameter to RunnableFileSystemResource cannot be null"));
        theRunnable = Constraint.isNotNull(runnable, "Runnable parameter to RunnableFileSystemResource cannot be null");
        thePrefix = "RunnableResource [" + getPath() + "]";

        try {
            callRunnable();
        } catch (final IOException ex) {
            throw new BeanCreationException(ex.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override public RunnableFileSystemResource createRelativeResource(final String relativePath) throws IOException {
        return new RunnableFileSystemResource(super.createRelative(relativePath).getFile(), theRunnable);
    }

    /** {@inheritDoc} */
    @Override public boolean exists() {
        try {
            callRunnable();
        } catch (final IOException e) {
            return false;
        }
        return super.exists();
    }

    /** {@inheritDoc} */
    @Override @Nonnull public InputStream getInputStream() throws IOException {
        callRunnable();
        return super.getInputStream();
    }

    /** {@inheritDoc} */
    @Override public long lastModified() throws IOException {
        callRunnable();
        return super.lastModified();
    }

    /**
     * Call the runnable and catch every event thrown.
     * 
     * @throws IOException if anything bad happens
     */
    protected void callRunnable() throws IOException {
        try {
            theRunnable.run();
        } catch (final Exception ex) {
            log.error("{} : Runnable failed", thePrefix, ex);
            throw new IOException(ex);
        }
    }
    
}