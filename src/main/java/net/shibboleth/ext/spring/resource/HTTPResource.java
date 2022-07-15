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

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.httpclient.HttpClientContextHandler;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Resource for looking up HTTP URLs. Allows injection and therefore configuration of an Apache {@link HttpClient}. Code
 * based on OpenSAML <code>HTTPMetadataResolver</code> and {@link org.springframework.core.io.UrlResource}.
 */
public class HTTPResource extends AbstractIdentifiedInitializableComponent implements Resource, BeanNameAware,
        InitializingBean, net.shibboleth.utilities.java.support.resource.Resource {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(HTTPResource.class);

    /** HTTP Client used to pull the resource. */
    @Nonnull private final HttpClient httpClient;
    
    /** URL to the Resource. */
    @Nonnull private final URL resourceURL;
    
    /** Optional handler to pre- and post-process context. */
    @Nullable private HttpClientContextHandler httpClientContextHandler;

    /**
     * Constructor.
     * 
     * @param client the client we use to connect with.
     * @param url URL to the remote data
     * @throws IOException if the URL was badly formed
     */
    public HTTPResource(@Nonnull @ParameterName(name="client") final HttpClient client,
            @Nonnull @NotEmpty @ParameterName(name="url") final String url) throws IOException {
        httpClient = Constraint.isNotNull(client, "HttpClient cannot be null");
        final String trimmedAddress =
                Constraint.isNotNull(StringSupport.trimOrNull(url), "Provided URL cannot be null or empty");
        resourceURL = new URL(trimmedAddress);

    }

    /**
     * Constructor.
     * 
     * @param client the client we use to connect with.
     * @param url URL to the remote data
     * @throws IOException if the URL was badly formed
     */
    public HTTPResource(@Nonnull @ParameterName(name="") final HttpClient client,
            @Nonnull @ParameterName(name="url") final URL url) throws IOException {
        httpClient = Constraint.isNotNull(client, "HttpClient cannot be null");
        resourceURL = Constraint.isNotNull(url, "Provided URL cannot be null or empty");

    }
    
    /**
     * Set a handler to manipulate the {@link HttpClientContext}.
     * 
     * @param handler the handler to install
     * 
     * @since 5.4.0
     */
    public void setHttpClientContextHandler(@Nonnull final HttpClientContextHandler handler) {
        checkSetterPreconditions();
        
        httpClientContextHandler = handler;
    }

    /**
     * Build the {@link HttpCacheContext} instance which will be used to invoke the {@link HttpClient} request.
     * 
     * @return a new instance of {@link HttpCacheContext}
     */
    @Nonnull protected HttpCacheContext buildHttpClientContext() {
        return HttpCacheContext.create();
    }

    /**
     * Print out to the log whether we hit the apache cache or not.
     * http://hc.apache.org/httpcomponents-client-ga/tutorial/html/caching.html
     * @param context the context of the request
     */
    protected void reportCachingStatus(final HttpCacheContext context) {
        final CacheResponseStatus responseStatus = context.getCacheResponseStatus();
        if (null == responseStatus) {
            log.debug("Non caching client provided");
            return;
        }
        switch (responseStatus) {
            case CACHE_HIT:
                log.debug("A response was generated from the cache with no requests sent upstream");
                break;
            case CACHE_MODULE_RESPONSE:
                log.debug("The response was generated directly by the caching module");
                break;
            case CACHE_MISS:
                log.debug("The response came from an upstream server");
                break;
            case VALIDATED:
                log.debug("The response was generated from the cache "
                        + "after validating the entry with the origin server");
                break;
            default:
                log.info("Unknown status {}", responseStatus.toString());
                break;
        }
    }

    /** {@inheritDoc} */
    @Override @Nonnull public InputStream getInputStream() throws IOException {
        final HttpGet httpGet = new HttpGet(resourceURL.toExternalForm());
        final HttpCacheContext context = buildHttpClientContext();
        
        if (httpClientContextHandler != null) {
            log.debug("Invoking HttpClientContextHandler prior to execution");
            httpClientContextHandler.invokeBefore(context, httpGet);
        }
        
        log.debug("Attempting to get data from remote resource '{}'", resourceURL);
        final HttpResponse response = httpClient.execute(httpGet, context);
        
        if (httpClientContextHandler != null) {
            log.debug("Invoking HttpClientContextHandler after execution");
            httpClientContextHandler.invokeAfter(context, httpGet);
        }
        
        reportCachingStatus(context);
        final int httpStatusCode = response.getStatusLine().getStatusCode();

        if (httpStatusCode != HttpStatus.SC_OK) {
            final String errMsg =
                    "Non-ok status code " + httpStatusCode + " returned from remote resource " + resourceURL;
            log.error(errMsg);
            closeResponse(response);
            throw new IOException(errMsg);
        }

        return new ConnectionClosingInputStream(response);
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

        log.debug("Attempting to fetch remote resource as '{}'", resourceURL);
        final HttpResponse response;
        try {
            response = getResourceHeaders();
        } catch (final IOException e) {
            return false;
        }
        final int httpStatusCode = response.getStatusLine().getStatusCode();

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
        } catch (final URISyntaxException ex) {
            throw new IOException("Invalid URI [" + resourceURL + "]", ex);
        }
    }

    /** {@inheritDoc} Based on {@link org.springframework.core.io.UrlResource}. */
    @Override public File getFile() throws IOException {
        throw new FileNotFoundException("HTTPResource cannot be resolved to absolute file path "
                + "because it does not reside in the file system: " + resourceURL);
    }

    /**
     * Attempts to fetch only the headers for a given resource. If HEAD requests are unsupported then a more costly GET
     * request is performed.
     * 
     * @return the response from the request
     * 
     * @throws IOException thrown if there is a problem contacting the resource
     */
    protected HttpResponse getResourceHeaders() throws IOException {
        final HttpUriRequest httpRequest = new HttpGet(resourceURL.toExternalForm());

        HttpResponse httpResponse = null;
        try {
            final HttpCacheContext context = buildHttpClientContext();
            
            if (httpClientContextHandler != null) {
                log.debug("Invoking HttpClientContextHandler prior to execution");
                httpClientContextHandler.invokeBefore(context, httpRequest);
            }
            
            httpResponse = httpClient.execute(httpRequest, context);
            
            if (httpClientContextHandler != null) {
                log.debug("Invoking HttpClientContextHandler after execution");
                httpClientContextHandler.invokeAfter(context, httpRequest);
            }
            
            reportCachingStatus(context);
            EntityUtils.consume(httpResponse.getEntity());
            return httpResponse;
        } catch (final IOException e) {
            throw new IOException("Error contacting remote resource " + resourceURL.toString(), e);
        } finally {
            closeResponse(httpResponse);
        }
    }

    /**
     * Send a Head to the client and interrogate the response for a particular response header.
     * 
     * @param what the repsonse header to look at
     * @return the value of that response, or null if things failed
     * @throws IOException from lower levels.
     */
    @Nullable protected String getResponseHeader(final String what) throws IOException {
        final HttpResponse response;

        log.debug("Attempting to fetch remote resource as '{}'", resourceURL);
        response = getResourceHeaders();
        final int httpStatusCode = response.getStatusLine().getStatusCode();

        if (httpStatusCode != HttpStatus.SC_OK) {
            final String errMsg =
                    "Non-ok status code " + httpStatusCode + " returned from remote resource " + resourceURL;
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

        final String response = getResponseHeader(HttpHeaders.CONTENT_LENGTH);
        if (null != response) {
            return Long.parseLong(response);
        }
        final String errMsg = "Response from remote resource " + resourceURL.toString() + 
                " did not contain a Content-Length header";
        log.error(errMsg);
        throw new IOException(errMsg);
    }

    /** {@inheritDoc} */
    @Override public long lastModified() throws IOException {
        final String response = getResponseHeader(HttpHeaders.LAST_MODIFIED);
        if (null != response) {
            return DateUtils.parseDate(response).getTime();
        }
        final String errMsg = "Response from remote resource " + resourceURL.toString() + 
                " did not contain a Last-Modified header";
        log.error(errMsg);
        throw new IOException(errMsg);
    }

    /** {@inheritDoc} Based on {@link org.springframework.core.io.UrlResource}. */
    @Override public HTTPResource createRelative(final String relativePath) throws IOException {
        final String path;
        if (relativePath.startsWith("/")) {
            path = relativePath.substring(1);
        } else {
            path = relativePath;
        }
        final HTTPResource result = new HTTPResource(httpClient, new URL(resourceURL, path));
        try {
            result.setId(getId()+"_"+relativePath);
            result.initialize();
        } catch (final ComponentInitializationException e) {
            throw new IOException(e);
        }
        return result;
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
    
    /**
     * Close the HTTP response.
     * 
     * @param response the HTTP response
     */
    protected void closeResponse(@Nullable final HttpResponse response) {
        try {
            if (response != null && response instanceof CloseableHttpResponse) {
                ((CloseableHttpResponse) response).close();
            }
        } catch (final IOException e) {
            log.error("Error closing HTTP response from '{}'", resourceURL.toExternalForm(), e);
        }
    }
    
    /**
     * A wrapper around the entity content {@link InputStream} represented by an {@link HttpResponse}
     * that closes the stream and the HttpResponse when {@link #close()} is invoked.
     */
     private static class ConnectionClosingInputStream extends InputStream {

        /** HTTP response that is being wrapped. */
        private final HttpResponse response;

        /** Stream owned by the given HTTP response. */
        private final InputStream stream;

        /**
         * Constructor.
         *
         * @param httpResponse HTTP method that was invoked
         * @throws IOException if there is a problem getting the entity content input stream from the response
         */
        public ConnectionClosingInputStream(@Nonnull final HttpResponse httpResponse) throws IOException {
            response = httpResponse;
            stream = response.getEntity().getContent();
        }

        /** {@inheritDoc} */
        @Override
        public int available() throws IOException {
            return stream.available();
        }

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
            stream.close();
            if (response instanceof CloseableHttpResponse) {
               ((CloseableHttpResponse)response).close();
            }
        }

        /** {@inheritDoc} */
        @Override
        public synchronized void mark(final int readLimit) {
            stream.mark(readLimit);
        }

        /** {@inheritDoc} */
        @Override
        public boolean markSupported() {
            return stream.markSupported();
        }

        /** {@inheritDoc} */
        @Override
        public int read() throws IOException {
            return stream.read();
        }

        /** {@inheritDoc} */
        @Override
        public int read(final byte[] b) throws IOException {
            return stream.read(b);
        }

        /** {@inheritDoc} */
        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return stream.read(b, off, len);
        }

        /** {@inheritDoc} */
        @Override
        public synchronized void reset() throws IOException {
            stream.reset();
        }

        /** {@inheritDoc} */
        @Override
        public long skip(final long n) throws IOException {
            return stream.skip(n);
        }
    }

}
