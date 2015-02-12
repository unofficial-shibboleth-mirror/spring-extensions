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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;
import net.shibboleth.utilities.java.support.xml.QNameSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A base class for {@link NamespaceHandler} implementations.
 * 
 * This code is heavily based on Spring's <code>NamespaceHandlerSupport</code>. The largest difference is that bean
 * definition parsers may be registered against either an elements name or schema type. During parser lookup the schema
 * type is preferred.
 */
public abstract class BaseSpringNamespaceHandler implements NamespaceHandler {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseSpringNamespaceHandler.class);

    /**
     * Stores the {@link BeanDefinitionParser} implementations keyed by the local name of the {@link Element Elements}
     * they handle.
     */
    private Map<QName, BeanDefinitionParser> parsers = new HashMap<>();

    /**
     * Stores the {@link BeanDefinitionDecorator} implementations keyed by the local name of the {@link Element
     * Elements} they handle.
     */
    private Map<QName, BeanDefinitionDecorator> decorators = new HashMap<>();

    /**
     * Stores the {@link BeanDefinitionParser} implementations keyed by the local name of the {@link Attr Attrs} they
     * handle.
     */
    private Map<QName, BeanDefinitionDecorator> attributeDecorators = new HashMap<>();

    /**
     * Decorates the supplied {@link Node} by delegating to the {@link BeanDefinitionDecorator} that is registered to
     * handle that {@link Node}.
     * 
     * @param node the node decorating a the given bean definition
     * @param definition the bean being decorated
     * @param parserContext the current parser context
     * 
     * @return the decorated bean definition
     */
    @Override
    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
        return findDecoratorForNode(node).decorate(node, definition, parserContext);
    }

    /**
     * Parses the supplied {@link Element} by delegating to the {@link BeanDefinitionParser} that is registered for that
     * {@link Element}.
     * 
     * @param element the element to be parsed into a bean definition
     * @param parserContext the context within which the bean definition is created
     * 
     * @return the bean definition created from the given element
     */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return findParserForElement(element).parse(element, parserContext);
    }

    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using the local name of the supplied
     * {@link Element}.
     * 
     * @param element the element to locate the bean definition parser for
     * 
     * @return the parser for the given bean element
     */
    protected BeanDefinitionParser findParserForElement(Element element) {
        BeanDefinitionParser parser = null;

        QName typeName = DOMTypeSupport.getXSIType(element);
        if (typeName != null) {
            log.trace("Attempting to find parser for element of type: {}", typeName);
            parser = parsers.get(typeName);
        }

        QName elementName = null;
        if (parser == null) {
            elementName = QNameSupport.getNodeQName(element);
            log.trace("Attempting to find parser with element name: {}", elementName);
            parser = parsers.get(elementName);
        }

        if (parser == null) {
            String msg = "Can not locate BeanDefinitionParser for element: " + elementName;
            if (typeName != null) {
                msg = msg + ", carrying xsi:type: " + typeName;
            }
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return parser;
    }

    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using the local name of the supplied
     * {@link Node}. Supports both {@link Element Elements} and {@link Attr Attrs}.
     * 
     * @param node the node to locate the decorator for
     * 
     * @return the decorator for the given node
     */
    protected BeanDefinitionDecorator findDecoratorForNode(Node node) {
        BeanDefinitionDecorator decorator = null;

        if (node instanceof Element) {
            decorator = decorators.get(DOMTypeSupport.getXSIType((Element) node));
            if (decorator == null) {
                decorator = decorators.get(QNameSupport.getNodeQName(node));
            }
        } else if (node instanceof Attr) {
            decorator = attributeDecorators.get(node.getLocalName());
        } else {
            throw new IllegalArgumentException("Can not decorate based on Nodes of type [" + node.getClass().getName()
                    + "]");
        }

        if (decorator == null) {
            throw new IllegalArgumentException("Can not locate BeanDefinitionDecorator for " + " ["
                    + node.getLocalName() + "]");
        }

        return decorator;
    }

    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionParser} to handle the specified element.
     * The element name is the local (non-namespace qualified) name.
     * 
     * @param elementNameOrType the element name or schema type the parser is for
     * @param parser the parser to register
     */
    protected void registerBeanDefinitionParser(QName elementNameOrType, BeanDefinitionParser parser) {
        parsers.put(elementNameOrType, parser);
    }

    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to handle the specified
     * element. The element name is the local (non-namespace qualified) name.
     * 
     * @param elementNameOrType the element name or schema type the parser is for
     * @param decorator the decorator to register
     */
    protected void registerBeanDefinitionDecorator(QName elementNameOrType, BeanDefinitionDecorator decorator) {
        decorators.put(elementNameOrType, decorator);
    }

    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to handle the specified
     * attribute. The attribute name is the local (non-namespace qualified) name.
     * 
     * @param attributeName the name of the attribute to register the decorator for
     * @param decorator the decorator to register
     */
    protected void registerBeanDefinitionDecoratorForAttribute(QName attributeName, BeanDefinitionDecorator decorator) {
        attributeDecorators.put(attributeName, decorator);
    }
}