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

import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.InMemoryCachingHttpClientBuilder;

/**
 * Factory bean to accumulate the parameters into a {@link InMemoryCachingHttpClientBuilder} and to then emit a
 * {@link org.apache.http.client.HttpClient}.
 */
public class InMemoryCachingHttpClientFactoryBean extends HttpClientFactoryBean {

    /** Constructor. */
    public InMemoryCachingHttpClientFactoryBean() {
        super();
    }

    /**
     * Set the maximum number of cached responses.
     * 
     * @param maxCacheEntries The maxCacheEntries to set.
     */
    public void setMaxCacheEntries(final int maxCacheEntries) {
        ((InMemoryCachingHttpClientBuilder) getHttpClientBuilder()).setMaxCacheEntries(maxCacheEntries);
    }

    /**
     * Set the maximum response body size, in bytes, that will be eligible for caching.
     * 
     * @param maxCacheEntrySize The maxCacheEntrySize to set.
     */
    public void setMaxCacheEntrySize(final long maxCacheEntrySize) {
        ((InMemoryCachingHttpClientBuilder) getHttpClientBuilder()).setMaxCacheEntrySize(maxCacheEntrySize);
    }

    /** {@inheritDoc} */
    @Override protected HttpClientBuilder createHttpClientBuilder() {
        return new InMemoryCachingHttpClientBuilder();
    }

}
