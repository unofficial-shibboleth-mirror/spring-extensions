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

package net.shibboleth.ext.spring.context;

import net.shibboleth.ext.spring.resource.ConditionalResourceResolver;
import net.shibboleth.ext.spring.util.AnnotationParameterNameDiscoverer;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * An extension of {@link GenericWebApplicationContext} that is biased in favor of the filesystem such that bare
 * resource paths are assumed to be files rather than classpath resources, and supports conditional resources.
 * 
 * @since 6.1.0
 */
public class FilesystemGenericWebApplicationContext extends GenericWebApplicationContext {

    /** Constructor. */
    public FilesystemGenericWebApplicationContext() {
        getDefaultListableBeanFactory().setParameterNameDiscoverer(new AnnotationParameterNameDiscoverer());
        addProtocolResolver(new ConditionalResourceResolver());
    }

    /**
     * Constructor.
     *
     * @param beanFactory bean factory
     */
    public FilesystemGenericWebApplicationContext(final DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
        beanFactory.setParameterNameDiscoverer(new AnnotationParameterNameDiscoverer());
        addProtocolResolver(new ConditionalResourceResolver());
    }

    /**
     * Constructor.
     *
     * @param context servlet context
     */
    public FilesystemGenericWebApplicationContext(final ServletContext context) {
        super(context);
        getDefaultListableBeanFactory().setParameterNameDiscoverer(new AnnotationParameterNameDiscoverer());
        addProtocolResolver(new ConditionalResourceResolver());
    }

    /**
     * Constructor.
     *
     * @param beanFactory bean factory
     * @param context servlet context
     */
    public FilesystemGenericWebApplicationContext(final DefaultListableBeanFactory beanFactory,
            final ServletContext context) {
        super(beanFactory, context);
        beanFactory.setParameterNameDiscoverer(new AnnotationParameterNameDiscoverer());
        addProtocolResolver(new ConditionalResourceResolver());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Overrides the standard behavior of path-only resources and treats them as file paths if the path exists. Note
     * that this differs from the ordinary Spring contexts that default to file paths because paths are treated as
     * absolute if they are in fact absolute.
     * </p>
     */
    @Override protected Resource getResourceByPath(final String path) {
        final Resource r = new FileSystemResource(path);
        if (r.exists()) {
            return r;
        }
        return super.getResourceByPath(path);
    }

}