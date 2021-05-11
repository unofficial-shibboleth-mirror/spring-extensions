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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ResourceLoader;

/**
 * Allows setting {@link Resource} properties using a string representing a Spring resource. The Spring resource is
 * retrieved by the application context. If the application context is null, then a
 * {@link PreferFileSystemResourceLoader} is used instead to get the resource.
 */
public class StringToResourceConverter implements Converter<String, Resource>, ApplicationContextAware {

    /** Application context. */
    @Nullable private ApplicationContext applicationContext;

    /** Log.  */
    @Nonnull private final Logger log = LoggerFactory.getLogger(StringToResourceConverter.class);

    /** {@inheritDoc} */
    public Resource convert(final String source) {
        final ResourceLoader loader =
                applicationContext == null ? new PreferFileSystemResourceLoader() : applicationContext;
        final Resource result = ResourceHelper.of(loader.getResource(source));
        
        if (source.endsWith(" ") || log.isDebugEnabled()) {
            if (!result.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("Resource at '{}' does not exist", source);
                }
                if (source.endsWith(" ")) {
                    log.warn("Missing path '{}' ends with a space, check for stray characters", source);
                }
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(final ApplicationContext context) {
        applicationContext = context;
    }

}