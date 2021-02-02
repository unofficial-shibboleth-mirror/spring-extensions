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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;

/**
 * This is the base parser for all our custom syntax.
 *   
 * It is a trivial extension of {@link AbstractCustomBeanDefinitionParser}, but allows
 * for a single point of change to our behaviors. 
 */
public class AbstractCustomBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractCustomBeanDefinitionParser.class);

    /** {@inheritDoc}
     * The override is to warn if there is an ID clash within the same context.
     * */
    protected void registerBeanDefinition(final BeanDefinitionHolder definition,
                                          final BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(definition.getBeanName())) {
            final String claz = definition.getBeanDefinition().getBeanClassName();
            log.warn("Duplicate Definition '{}' of type '{}'", definition.getBeanName(), claz);
        }
        super.registerBeanDefinition(definition, registry);
    }
}
