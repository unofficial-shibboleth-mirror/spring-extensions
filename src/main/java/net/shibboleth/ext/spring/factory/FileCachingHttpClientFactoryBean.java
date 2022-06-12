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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.httpclient.FileCachingHttpClientBuilder;

/**
 * Factory bean version of {@link FileCachingHttpClientBuilder}.
 * 
 * <p>This is different than the other factories in order to limit non-singleton use
 * and implement init/destroy. This can't handle prototypes but that makes no sense when you
 * consider a shared cache in one directory wouldn't work anyway.</p>
 */
public class FileCachingHttpClientFactoryBean extends FileCachingHttpClientBuilder
        implements FactoryBean<HttpClient>, DisposableBean {

    /** Our captive client in singleton cases. */
    @Nullable private HttpClient singletonInstance;

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return true;
    }
    
    /** {@inheritDoc} */
    public Class<HttpClient> getObjectType() {
        return HttpClient.class;
    }
    
    /** {@inheritDoc} */
    public void destroy() {
        if (singletonInstance instanceof DestructableComponent) {
            ((DestructableComponent) singletonInstance).destroy();
        }
    }

    /** {@inheritDoc} */
    public HttpClient getObject() throws Exception {
        if (singletonInstance == null) {
            final HttpClient theBean = buildClient();
            if (theBean instanceof InitializableComponent) {
                ((InitializableComponent) theBean).initialize();
            }
            singletonInstance = theBean;
        }
        
        return singletonInstance;
    }
    
}