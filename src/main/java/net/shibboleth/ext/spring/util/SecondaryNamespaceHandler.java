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

import javax.xml.namespace.QName;

import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.w3c.dom.Element;

/** A secondary namespace handler to allow us to stack multiple parsers.  */
public abstract class SecondaryNamespaceHandler {
    
    /**
     * Stores the {@link BeanDefinitionParser} implementations keyed by the local name of the {@link Element Elements}
     * they handle.
     */
    private Map<QName, BeanDefinitionParser> parsers; 
    
    /** Initialize the handler, called when a {@link BaseSpringNamespaceHandler}
     * calls {@link BaseSpringNamespaceHandler#initializeOtherHandlers(String)}.
     *  
     * @param theParsers 
     */
    protected void init(final Map<QName, BeanDefinitionParser> theParsers) {
        parsers = theParsers;
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
     * Subclasses implement this call and in it register the {@link BeanDefinitionParser}s 
     * via calls to {@link #registerBeanDefinitionParser(QName, BeanDefinitionParser) }.
     */
    public abstract void doInit();
}
