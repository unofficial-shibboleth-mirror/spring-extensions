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

import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.env.Environment;
import org.w3c.dom.Element;

/**
 * An extension to the standard {@link BeanDefinitionParserDelegate} that adds support for retrieving
 * {@link NamespaceHandler} by schema type, as well as element QName, when resolving custom elements. In the case where
 * a {@link NamespaceHandler} is registered for both the schema type and element QName for a custom element the schema
 * registered handler is preferred.
 */
public class SchemaTypeAwareBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate {

    /**
     * Constructor.
     * 
     * @param readerContext current XML reader context
     * @param environment current environment
     */
    public SchemaTypeAwareBeanDefinitionParserDelegate(XmlReaderContext readerContext, Environment environment) {
        super(readerContext, environment);
    }

    /** {@inheritDoc} */
    public BeanDefinition parseCustomElement(Element element) {
        return parseCustomElement(element, null);
    }

    /** {@inheritDoc} */
    public BeanDefinition parseCustomElement(Element element, BeanDefinition containingBd) {
        String namespaceUri = element.getNamespaceURI();
        if (DOMTypeSupport.hasXSIType(element)) {
            namespaceUri = DOMTypeSupport.getXSIType(element).getNamespaceURI();
        }

        NamespaceHandler handler = getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler == null) {
            error("Unable to locate NamespaceHandler for namespace [" + namespaceUri + "]", element);
            return null;
        }

        return handler.parse(element, new ParserContext(getReaderContext(), this, containingBd));
    }
}
