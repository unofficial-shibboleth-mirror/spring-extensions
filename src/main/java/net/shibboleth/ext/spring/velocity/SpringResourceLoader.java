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

package net.shibboleth.ext.spring.velocity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Velocity ResourceLoader adapter that loads via a Spring ResourceLoader.
 * Used by VelocityEngineFactory for any resource loader path that cannot
 * be resolved to a {@code java.io.File}.
 *
 * <p>Note that this loader does not allow for modification detection:
 * Use Velocity's default FileResourceLoader for {@code java.io.File}
 * resources.
 *
 * <p>Expects "spring.resource.loader" and "spring.resource.loader.path"
 * application attributes in the Velocity runtime: the former of type
 * {@code org.springframework.core.io.ResourceLoader}, the latter a String.
 *
 * @author Juergen Hoeller
 * @see VelocityEngineFactory#setResourceLoaderPath
 * @see org.springframework.core.io.ResourceLoader
 * @see org.apache.velocity.runtime.resource.loader.FileResourceLoader
 * 
 * @since 6.0.0
 */
public class SpringResourceLoader extends ResourceLoader {

    /** Constant identifying resource loader name. */
    @Nonnull @NotEmpty public static final String NAME = "spring";

    /** Constant identifying resource loader class name. */
    @Nonnull @NotEmpty public static final String SPRING_RESOURCE_LOADER_CLASS = "spring.resource.loader.class";

    /** Constant identifying caching property. */
    @Nonnull @NotEmpty public static final String SPRING_RESOURCE_LOADER_CACHE = "spring.resource.loader.cache";

    /** Constant identifying {@link ResourceLoader} instance. */
    @Nonnull @NotEmpty public static final String SPRING_RESOURCE_LOADER = "spring.resource.loader";

    /** Constant identifying resource loader path. */
    @Nonnull @NotEmpty public static final String SPRING_RESOURCE_LOADER_PATH = "spring.resource.loader.path";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SpringResourceLoader.class);

    /** Underlying Spring resource loader. */
    @Nullable private org.springframework.core.io.ResourceLoader resourceLoader;

    /** Resource loader paths. */
    @Nullable private String[] resourceLoaderPaths;

    /** {@inheritDoc} */
    @Override
    public void init(final ExtProperties configuration) {
        resourceLoader =
                (org.springframework.core.io.ResourceLoader) rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER);
        final String resourceLoaderPath = (String) rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER_PATH);
        if (resourceLoader == null) {
            throw new IllegalArgumentException(
                    "'resourceLoader' application attribute must be present for SpringResourceLoader");
        }
        if (resourceLoaderPath == null) {
            throw new IllegalArgumentException(
                    "'resourceLoaderPath' application attribute must be present for SpringResourceLoader");
        }
        resourceLoaderPaths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
        for (int i = 0; i < resourceLoaderPaths.length; i++) {
            final String path = resourceLoaderPaths[i];
            if (!path.endsWith("/")) {
                resourceLoaderPaths[i] = path + "/";
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("SpringResourceLoader for Velocity: using resource loader {} and resource loader paths {}",
                    resourceLoader, Arrays.asList(resourceLoaderPaths));
        }
    }

    /** {@inheritDoc} */
    @Override
    public Reader getResourceReader(final String source, final String encoding)
            throws ResourceNotFoundException {
        log.debug("Looking for Velocity resource with name '{}'", source);
        for (final String resourceLoaderPath : resourceLoaderPaths) {
            final org.springframework.core.io.Resource resource =
                    resourceLoader.getResource(resourceLoaderPath + source);
            try {
                return new InputStreamReader(resource.getInputStream(), encoding);
            } catch (final IOException ex) {
                log.debug("Could not find Velocity resource: {}", resource);
            }
        }
        throw new ResourceNotFoundException(
                "Could not find resource [" + source + "] in Spring resource loader path");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSourceModified(final Resource resource) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public long getLastModified(final Resource resource) {
        return 0;
    }

}