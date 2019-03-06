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

import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.FactoryBean;

import net.shibboleth.utilities.java.support.httpclient.InMemoryCachingHttpClientBuilder;

/**
 * Factory bean version of {@link InMemoryCachingHttpClientBuilder}.
 */
public class InMemoryCachingHttpClientFactoryBean extends InMemoryCachingHttpClientBuilder
    implements FactoryBean<HttpClient> {

    /** Singleton flag. */
    private boolean singleton;
    
    /** Our captive client in singleton cases. */
    @Nullable private HttpClient singletonInstance;

    /** Constructor. */
    public InMemoryCachingHttpClientFactoryBean() {
        singleton = true;
    }
    
    /**
     * Set if a singleton should be created, or a new object on each request
     * otherwise. Default is {@code true} (a singleton).
     * 
     * @param flag flag to set
     */
    public void setSingleton(final boolean flag) {
        singleton = flag;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return singleton;
    }

    /** {@inheritDoc} */
    public Class<HttpClient> getObjectType() {
        return HttpClient.class;
    }
    
    /** {@inheritDoc} */
    public synchronized HttpClient getObject() throws Exception {
        if (isSingleton()) {
            if (singletonInstance != null) {
                return singletonInstance;
            }
            
            singletonInstance = buildClient();
            return singletonInstance;
        } else {
            return buildClient();
        }
    }

}