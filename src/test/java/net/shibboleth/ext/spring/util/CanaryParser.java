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

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Custom namespace parser for JSSH-20 test canary.
 */
public class CanaryParser extends BaseSpringNamespaceHandler {
    
    /**
     * Test namespace.
     */
    @Nonnull @NotEmpty protected static final String NAMESPACE = "urn:mace:shibboleth:2.0:canary";
    
    /** {@inheritDoc} */
    @Override
    public void init() {
        registerBeanDefinitionParser(new QName(NAMESPACE, "OurElement"), new OurElementParser());
    }

    static class OurElementParser implements BeanDefinitionParser {
        
        /** {@inheritDoc} */
        public BeanDefinition parse(@Nonnull final Element config, @Nonnull final ParserContext parserContext) {
            return null;
        }
    }
    
}