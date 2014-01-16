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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;

/**
 * Resource for looking up HTTP URLs. Allows injection and therefore configuration of an Apache {@link HttpClient}. Code
 * based in {@link org.opensaml.saml.metadata.resolver.impl.HTTPMetadataResolver} and
 * {@link org.springframework.core.io.UrlResource}.
 */
public class HTTPResource extends AbstractIdentifiableInitializableComponent implements Resource, BeanNameAware,
        InitializingBean, net.shibboleth.utilities.java.support.resource.Resource {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(HTTPResource.class);

    /** HTTP Client used to pull the metadata. */
    private final HttpClient httpClient;

    /** URI to the Resource. */
    // private final URI resourceURI;

    /** URL to the Resource. */
    private final URL resourceURL;

    /** HttpClient credentials provider. */
    private BasicCredentialsProvider credentialsProvider;

    /**
     * Constructor.
     * 
     * @param client the client we use to connect with.
     * @param remoteURL URL to the remote data
     * @throws IOException if the URL was badly formed
     */
    public HTTPResource(@Nonnull HttpClient client, @NotEmpty @Nonnull String remoteURL) throws IOException {
        httpClient = Constraint.isNotNull(client, "The Client must not be null");
        final String trimmedAddress =
                Constraint
                        .isNotNull(StringSupport.trimOrNull(remoteURL), "Provided URL must be non empty and non null");
        resourceURL = new URL(trimmedAddress);

    }

    /**
     * Constructor.
     * 
     * @param client the client we use to connect with.
     * @param remoteURL URL to the remote data
     * @throws IOException if the URL was badly formed
     */
    public HTTPResource(@Nonnull HttpClient client, @Nonnull URL remoteURL) throws IOException {
        httpClient = Constraint.isNotNull(client, "The Client must not be null");
        resourceURL = Constraint.isNotNull(remoteURL, "Provided URL must be non empty and non null");

    }

    /**
     * Build the {@link HttpClientContext} instance which will be used to invoke the {@link HttpClient} request.
     * 
     * @return a new instance of {@link HttpClientContext}
     */
    protected HttpClientContext buildHttpClientContext() {
        HttpClientContext context = HttpClientContext.create();
        if (credentialsProvider != null) {
            context.setCredentialsProvider(credentialsProvider);
        }
        return context;
    }

    /** {@inheritDoc} */
    @Override public InputStream getInputStream() throws IOException {
        final HttpGet httpGet = new HttpGet(resourceURL.toExternalForm());
        final HttpClientContext context = buildHttpClientContext();
        HttpResponse response = null;

        log.debug("Attempting to get data from '{}'", resourceURL);
        response = httpClient.execute(httpGet, context);
        int httpStatusCode = response.getStatusLine().getStatusCode();

        if (httpStatusCode != HttpStatus.SC_OK) {
            String errMsg =
                    "Non-ok status code " + httpStatusCode + " returned from remote metadata source " + resourceURL;
            log.error(errMsg);
            throw new IOException(errMsg);
        }

        return response.getEntity().getContent();
    }

    /** {@inheritDoc} */
    @Override public void afterPropertiesSet() throws Exception {
        initialize();
    }

    /** {@inheritDoc} */
    @Override public void setBeanName(final String name) {
        setId(name);
    }

    /** {@inheritDoc} */
    @Override public boolean exists() {
        final HttpHead httpHead = new HttpHead(resourceURL.toString());
        final HttpClientContext context = buildHttpClientContext();
        final HttpResponse response;

        log.debug("Attempting to fetch metadata for resource as '{}'", resourceURL);
        try {
            response = httpClient.execute(httpHead, context);
        } catch (IOException e) {
            return false;
        }
        int httpStatusCode = response.getStatusLine().getStatusCode();

        return httpStatusCode == HttpStatus.SC_OK;
    }

    /** {@inheritDoc} */
    @Override public boolean isReadable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean isOpen() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public URL getURL() throws IOException {
        return resourceURL;
    }

    /** {@inheritDoc} */
    @Override public URI getURI() throws IOException {
        try {
            return resourceURL.toURI();
        } catch (URISyntaxException ex) {
            throw new NestedIOException("Invalid URI [" + resourceURL + "]", ex);
        }
    }

    /** {@inheritDoc} Based on {@link org.springframework.core.io.UrlResource}. */
    @Override public File getFile() throws IOException {
        throw new FileNotFoundException("HTTPResource cannot be resolved to absolute file path "
                + "because it does not reside in the file system: " + resourceURL);
    }

    /**
     * Send a Head to the client and interrogate the response for a particular response header.
     * 
     * @param what the repsonse header to look at
     * @return the value of that response, or null if things failed
     * @throws IOException from lower levels.
     */
    @Nullable protected String getResponseHeader(String what) throws IOException {
        final HttpHead httpHead = new HttpHead(resourceURL.toString());
        final HttpClientContext context = buildHttpClientContext();
        final HttpResponse response;

        log.debug("Attempting to fetch metadata for resource as '{}'", resourceURL);
        response = httpClient.execute(httpHead, context);
        int httpStatusCode = response.getStatusLine().getStatusCode();

        if (httpStatusCode != HttpStatus.SC_OK) {
            final String errMsg =
                    "Non-ok status code " + httpStatusCode + " returned from remote metadata source " + resourceURL;
            log.error(errMsg);
            throw new IOException(errMsg);
        }

        final Header httpHeader = response.getFirstHeader(what);
        if (httpHeader != null) {
            return httpHeader.getValue();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override public long contentLength() throws IOException {

        String response = getResponseHeader("Content-Length");
        if (null != response) {
            return Long.parseLong(response);
        }
        final String errMsg = "Response from " + resourceURL.toString() + " did not contain a Content-Length header";
        log.error(errMsg);
        throw new IOException(errMsg);
    }

    /** {@inheritDoc} */
    @Override public long lastModified() throws IOException {
        String response = getResponseHeader("Last-Modified");
        if (null != response) {
            return DateUtils.parseDate(response).getTime();
        }
        final String errMsg = "Response from " + resourceURL.toString() + " did not contain a Last-Modified header";
        log.error(errMsg);
        throw new IOException(errMsg);
    }

    /** {@inheritDoc} Based on {@link org.springframework.core.io.UrlResource}. */
    @Override public HTTPResource createRelative(final String relativePath) throws IOException {
        String path;
        if (relativePath.startsWith("/")) {
            path = relativePath.substring(1);
        } else {
            path = relativePath;
        }
        return new HTTPResource(httpClient, new URL(resourceURL, path));
    }

    /** {@inheritDoc} */
    @Override public net.shibboleth.utilities.java.support.resource.Resource createRelativeResource(
            final String relativePath) throws IOException {

        return createRelative(relativePath);
    }

    /**
     * {@inheritDoc} This implementation returns the name of the file that this URL refers to.
     * 
     * @see java.net.URL#getFile()
     * @see java.io.File#getName()
     */
    @Override public String getFilename() {
        return new File(resourceURL.getFile()).getName();
    }

    /** {@inheritDoc} */
    @Override public String getDescription() {
        final StringBuilder builder = new StringBuilder("HTTPResource [").append(resourceURL.toString()).append(']');
        return builder.toString();

    }

}
