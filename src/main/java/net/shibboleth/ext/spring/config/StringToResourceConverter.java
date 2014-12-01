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

package net.shibboleth.ext.spring.config;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * Allows setting {@link Resource} properties using a string representing a Spring resource. The Spring resource is
 * retrieved by the application context. If the application context is null, then a {@link DefaultResourceLoader} is
 * used instead to get the resource.
 */
public class StringToResourceConverter implements Converter<String, Resource>, ApplicationContextAware {

    /** Application context. */
    private ApplicationContext applicationContext;

    /** {@inheritDoc} */
    public Resource convert(String source) {
        final ResourceLoader loader = applicationContext == null ? new DefaultResourceLoader() : applicationContext;
        return ResourceHelper.of(loader.getResource(source));
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

}