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

import java.io.IOException;

import javax.annotation.Nonnull;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.util.StringUtils;

import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;

/**
 * Version of {@link DeferPlaceholderFileSystemXmlWebApplicationContext} which does not necessarily assume that file
 * list are space separated.
 */
public class DelimiterAwareApplicationContext extends DeferPlaceholderFileSystemXmlWebApplicationContext {

    /** {@inheritDoc} */
    @Override public void setConfigLocation(final String location) {
        setConfigLocations(StringUtils.tokenizeToStringArray(location, getDelimiters()));
    }

    /**
     * Get the delimiters we will split the config locations on. All the usual suspects except space.
     * 
     * @return the delimiters
     */
    @Nonnull protected String getDelimiters() {
        return ",;\t\n";
    }

    /**
     * {@inheritDoc}
     * 
     * The override is necessary to replace the bean definition reader with a non-broken version that honors
     * the context's ResourceLoader instead of supplanting it.
     */
    @Override
    protected void loadBeanDefinitions(final DefaultListableBeanFactory beanFactory)
            throws BeansException, IOException {
        // Create a new XmlBeanDefinitionReader for the given BeanFactory.
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(beanFactory);

        // Configure the bean definition reader with this context's
        // resource loading environment.
        beanDefinitionReader.setEnvironment(getEnvironment());
        beanDefinitionReader.setResourceLoader(this);
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

        // Allow a subclass to provide custom initialization of the reader,
        // then proceed with actually loading the bean definitions.
        initBeanDefinitionReader(beanDefinitionReader);
        loadBeanDefinitions(beanDefinitionReader);
    }

}