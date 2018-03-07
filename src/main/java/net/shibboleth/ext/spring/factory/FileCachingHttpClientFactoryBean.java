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

import java.util.ArrayList;
import java.util.List;

import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.httpclient.FileCachingHttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

import org.apache.http.client.HttpClient;

/**
 * Factory bean to accumulate the parameters into a {@link FileCachingHttpClientBuilder} 
 * and to then emit a {@link org.apache.http.client.HttpClient}.
 * 
 * <p>This class will likely either be removed or moved into an implementation package.
 * Use {@link FileCachingHttpClientBuilder} instead.</p>
 * 
 * @deprecated
 */
public class FileCachingHttpClientFactoryBean extends HttpClientFactoryBean {
    
    /** List of HttpClients produced by this factory, used to invoke their destroy() 
     * when this factory instances is destroy()-ed. */
    private List<HttpClient> clientRefs;

    /** Constructor. */
    public FileCachingHttpClientFactoryBean() {
        clientRefs = new ArrayList<>();
    }

    /**
     * Set the cache directory path.
     * 
     * @param cacheDirectory The cacheDirectory to set.
     */
    public void setCacheDirectory(final String cacheDirectory) {
        ((FileCachingHttpClientBuilder)getHttpClientBuilder()).setCacheDirectory(cacheDirectory);
    }

    /**
     * Set the maximum number of cached responses.
     * 
     * @param maxCacheEntries The maxCacheEntries to set.
     */
    public void setMaxCacheEntries(final int maxCacheEntries) {
        ((FileCachingHttpClientBuilder)getHttpClientBuilder()).setMaxCacheEntries(maxCacheEntries);
    }

    /**
     * Set the maximum response body size, in bytes, that will be eligible for caching.
     * 
     * @param maxCacheEntrySize The maxCacheEntrySize to set.
     */
    public void setMaxCacheEntrySize(final long maxCacheEntrySize) {
        ((FileCachingHttpClientBuilder)getHttpClientBuilder()).setMaxCacheEntrySize(maxCacheEntrySize);
    }

    /** {@inheritDoc} */
    @Override
    protected HttpClientBuilder createHttpClientBuilder() {
        return new FileCachingHttpClientBuilder();
    }

    /** {@inheritDoc} */
    @Override
    protected HttpClient doCreateInstance() throws Exception {
        final HttpClient client = super.doCreateInstance();
        synchronized(this) {
            if (client instanceof InitializableComponent) {
                final InitializableComponent component = (InitializableComponent) client;
                if (!component.isInitialized()) {
                   component.initialize(); 
                }
            }
            clientRefs.add(client);
        }
        return client;
    }
    
}