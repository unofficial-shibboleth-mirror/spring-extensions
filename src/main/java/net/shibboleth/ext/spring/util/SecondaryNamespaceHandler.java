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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.w3c.dom.Element;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A secondary namespace handler to allow us to stack multiple parsers.
 * 
 * @since 7.0.0
 */
public abstract class SecondaryNamespaceHandler {
    
    /**
     * Stores the {@link BeanDefinitionParser} implementations keyed by the local name of the {@link Element Elements}
     * they handle.
     */
    @Nullable @NonnullElements private Map<QName, BeanDefinitionParser> parsers; 
    
    /**
     * Initialize the handler, called automatically as part of {@link BaseSpringNamespaceHandler#init()}
     * if the secondary handler qualifier was specified during
     * {@link BaseSpringNamespaceHandler#BaseSpringNamespaceHandler(String)}.
     *  
     * @param theParsers the parsers to use.
     */
    protected void init(@Nonnull @NonnullElements final Map<QName, BeanDefinitionParser> theParsers) {
        parsers = Constraint.isNotNull(theParsers, "Parser map cannot be null");
        doInit();
    }
    
    /**
     * Subclasses call this to register the supplied {@link BeanDefinitionParser} to handle the specified element.
     * The element name is the local (non-namespace qualified) name.
     * 
     * @param elementNameOrType the element name or schema type the parser is for
     * @param parser the parser to register
     */
    protected void registerBeanDefinitionParser(final QName elementNameOrType, final BeanDefinitionParser parser) {
        parsers.put(elementNameOrType, parser);
    }

    /**
     * Subclasses implement this method and in it register the {@link BeanDefinitionParser}s 
     * via calls to {@link #registerBeanDefinitionParser(QName, BeanDefinitionParser) }.
     */
    public abstract void doInit();

}