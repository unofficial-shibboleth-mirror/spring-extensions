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

package net.shibboleth.ext.spring.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * An extension to the standard {@link DefaultBeanDefinitionDocumentReader} that uses a
 * {@link SchemaTypeAwareBeanDefinitionParserDelegate} delegate for processing bean definitions.
 */
public class SchemaTypeAwareBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

    /**
     * {@inheritDoc}
     * 
     * This override prevents the default behavior from kicking in if the original resource location
     * is directly usable by the installed {@link ResourceLoader}.
     */
    @Override
    protected void importBeanDefinitionResource(final Element ele) {
        String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
        if (!StringUtils.hasText(location)) {
            getReaderContext().error("Resource location must not be empty", ele);
            return;
        }

        // Resolve system properties: e.g. "${user.dir}"
        location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

        final Set<Resource> actualResources = new LinkedHashSet<>(4);

        final Resource r = getReaderContext().getResourceLoader().getResource(location);
        if (r.exists()) {
            final int importCount = getReaderContext().getReader().loadBeanDefinitions(r);
            actualResources.add(r);
            if (logger.isTraceEnabled()) {
                logger.trace("Imported " + importCount + " bean definitions from location [" + location + "]");
            }
            final Resource[] actResArray = actualResources.toArray(new Resource[0]);
            getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
            return;
        }
        
        logger.info("Resource location [" + location + "] does not exist, delegating to default behavior");
        super.importBeanDefinitionResource(ele);
    }

    /** {@inheritDoc} */
    @Override protected BeanDefinitionParserDelegate createDelegate(final XmlReaderContext readerContext,
            final Element root,
            final BeanDefinitionParserDelegate parentDelegate) {
        final BeanDefinitionParserDelegate delegate =
                new SchemaTypeAwareBeanDefinitionParserDelegate(readerContext);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }
    
}
