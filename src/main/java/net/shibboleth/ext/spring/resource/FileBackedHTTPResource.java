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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.google.common.io.ByteStreams;

/**
 * A resource representing a file read from an HTTP(S) location. Every time the file is successfully read from the URL
 * location it is written to a backing file. If the file can not be read from the URL it is read from this backing file,
 * if available.
 * 
 */
public class FileBackedHTTPResource extends HTTPResource {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FileBackedHTTPResource.class);

    /** Backing resource file. */
    @Nonnull private final Resource backingResource;

    /**
     * Constructor.
     * 
     * @param client the client we use to connect with.
     * @param url URL to the remote data
     * @param resource the file to use as backing store
     * @throws IOException if the URL was badly formed
     * @deprecated use {@link #FileBackedHTTPResource(String, HttpClient, String)}
     */
    @Deprecated public FileBackedHTTPResource(@Nonnull @ParameterName(name="client") final HttpClient client, 
            @NotEmpty @Nonnull @ParameterName(name="url") final String url,
            @Nonnull  @ParameterName(name="resource") final Resource resource) throws IOException {
        super(client, url);
        backingResource = Constraint.isNotNull(resource, "Backing resource must not be null");
        if (null == resource.getFile()) {
            throw new IOException("Backing resource has to be file backed");
        }
        
        DeprecationSupport.warn(ObjectType.METHOD, "FileBackedHTTPResource constructor with resource argument", null,
                "FileBackedHTTPResource constructor with backingFile argument");
    }

    /**
     * Constructor.
     * 
     * @param client the client we use to connect with.
     * @param url URL to the remote data
     * @param resource the file to use as backing store
     * @throws IOException if the URL was badly formed
     * @deprecated use {@link #FileBackedHTTPResource(String, HttpClient, URL)}
     */
    @Deprecated public FileBackedHTTPResource(@Nonnull @ParameterName(name="client") final HttpClient client, 
            @Nonnull @NotEmpty @ParameterName(name="url") final URL url, 
            @Nonnull @ParameterName(name="resource") final Resource resource)
            throws IOException {
        super(client, url);
        backingResource = Constraint.isNotNull(resource, "Backing resource must not be null");
        if (null == resource.getFile()) {
            throw new IOException("Backing resource has to be file backed");
        }
        
        DeprecationSupport.warn(ObjectType.METHOD, "FileBackedHTTPResource constructor with resource argument", null,
                "FileBackedHTTPResource constructor with backingFile argument");
    }

    /**
     * Constructor.
     * 
     * @param backingFile the file to use as backing store
     * @param client the client we use to connect with.
     * @param url URL to the remote data
     * @throws IOException if the URL was badly formed
     */
    public FileBackedHTTPResource(@Nonnull @ParameterName(name="backingFile") final String backingFile,
            @Nonnull @ParameterName(name="client") final HttpClient client, 
            @NotEmpty @Nonnull @ParameterName(name="url") final String url) throws IOException {
        super(client, url);
        Constraint.isNotNull(backingFile, "File Name must not be null");
        final File file = new File(backingFile);
        backingResource = new FileSystemResource(file);
    }

    /**
     * Constructor.
     * 
     * @param backingFile the file to use as backing store
     * @param client the client we use to connect with.
     * @param url URL to the remote data
     * @throws IOException if the URL was badly formed
     */
    public FileBackedHTTPResource(@Nonnull  @ParameterName(name="backingFile") final String backingFile, 
            @Nonnull @ParameterName(name="client") final HttpClient client,
            @Nonnull  @ParameterName(name="url") final URL url)
            throws IOException {
        super(client, url);
        Constraint.isNotNull(backingFile, "File Name must not be null");
        final File file = new File(backingFile);
        backingResource = new FileSystemResource(file);
    }

    /**
     * saveAndClone. Read the contents into memory and then write out to the backing file. Finally
     * 
     * @param input the input stream
     * @return the cloned stream.
     * @throws IOException if an error happens. If the backing file might have been corrupted we delete it.
     */

    protected InputStream saveAndClone(final InputStream input) throws IOException {
        try (final FileOutputStream out = new FileOutputStream(backingResource.getFile())) {
            log.debug("{}: Copying file.", getDescription());
            ByteStreams.copy(input, out);
            log.debug("{}: Copy done.", getDescription());
        } catch (final IOException e) {
            // try to tidy up
            backingResource.getFile().delete();
            log.error("{}: Copy failed", getDescription(), e);
            throw e;
        } finally {
            input.close();
        }
        return new FileInputStream(backingResource.getFile());
    }

    /** {@inheritDoc} */
    @Override @Nonnull public InputStream getInputStream() throws IOException {
        try {
            final InputStream stream = super.getInputStream();
            return saveAndClone(stream);
        } catch (final IOException ex) {
            log.debug("{} Error obtaining HTTPResource InputStream or creating backing file", getDescription(), ex);
            log.warn("{} HTTP resource was inaccessible for getInputStream(), trying backing file.", getDescription());
            try {
                return new FileInputStream(backingResource.getFile());
            } catch (final IOException e) {
                log.error("FileBackedHTTPResource {}: Could not read backing file", getDescription(), e);
                throw e;
            }
        }
    }

    /** {@inheritDoc} */
    @Override public boolean exists() {

        log.debug("{}: Attempting to fetch HTTP resource", getDescription());
        final HttpResponse response;
        try {
            response = getResourceHeaders();
        } catch (final IOException e) {
            log.info("{}: Could not reach URL, trying file", getDescription(), e);
            return backingResource.exists();
        }
        final int httpStatusCode = response.getStatusLine().getStatusCode();

        if (httpStatusCode == HttpStatus.SC_OK) {
            return true;
        }
        return backingResource.exists();
    }

    /** {@inheritDoc} */
    @Override public long contentLength() throws IOException {

        try {
            return super.contentLength();
        } catch (final IOException e) {
            log.info("{}: Could not reach URL, trying file", getDescription(), e);
            return backingResource.contentLength();
        }
    }

    /** {@inheritDoc} */
    @Override public long lastModified() throws IOException {
        try {
            return super.lastModified();
        } catch (final IOException e) {
            log.info("{}: Could not reach URL, trying file", getDescription(), e);
            return backingResource.lastModified();
        }
    }

    /** {@inheritDoc} */
    @Override public HTTPResource createRelative(final String relativePath) throws IOException {
        log.warn("{}: Relative resources are not file backed");
        return super.createRelative(relativePath);
    }

    /** {@inheritDoc} */
    @Override public String getDescription() {
        String urlAsString;
        try {
            urlAsString = getURL().toString();
        } catch (final IOException e) {
            urlAsString = "<unknown>";
        }

        final StringBuilder builder =
                new StringBuilder("FileBackedHTTPResource [").append(urlAsString).append('|')
                        .append(backingResource.getDescription()).append(']');
        return builder.toString();
    }
    
}