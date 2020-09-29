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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale.LanguageRange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.google.common.base.Strings;

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.net.HttpServletSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import net.shibboleth.utilities.java.support.xml.XMLConstants;

/**
 * Helper class for performing some common Spring-related functions.
 */
public final class SpringSupport {

    /** Spring beans element name. */
    @Nonnull public static final QName SPRING_BEANS_ELEMENT_NAME = new QName(
            "http://www.springframework.org/schema/beans", "beans");

    /** Logger. */
    @Nonnull static final Logger LOG = LoggerFactory.getLogger(SpringSupport.class);

    /** Constructor. */
    private SpringSupport() {

    }

    /**
     * Parse list of elements into bean definitions which are inserted into the parent context.
     *
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     *
     */
    @Nullable public static void parseCustomElements(
            @Nullable @NonnullElements final Collection<Element> elements, @Nonnull final ParserContext parserContext) {
        if (elements == null) {
            return;
        }

        for (final Element e : elements) {
            if (e != null) {
                parseCustomElement(e, parserContext, null, false);
            }
        }
    }

    /**
     * Parse list of elements into bean definitions.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * @param parentBuilder the builder we are going to insert into
     * 
     * @return list of bean definitions
     */
    @Nullable public static ManagedList<BeanDefinition> parseCustomElements(
            @Nullable @NonnullElements final Collection<Element> elements,
            @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder parentBuilder) {

        if (elements == null) {
            return null;
        }
        Constraint.isNotNull(parentBuilder, "parentBuilder must not be null");

        final ManagedList<BeanDefinition> definitions = new ManagedList<>(elements.size());
        for (final Element e : elements) {
            if (e != null) {
                definitions.add(parseCustomElement(e, parserContext, parentBuilder, false));
            }
        }

        return definitions;
    }


    /**
     * Parse list of elements into bean definitions and set the lazy-init flag.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * 
     * @since 6.0.0
     */
    @Nullable public static void parseLazyInitCustomElements(
            @Nullable @NonnullElements final Collection<Element> elements, @Nonnull final ParserContext parserContext) {
        if (elements == null) {
            return;
        }

        for (final Element e : elements) {
            if (e != null) {
                parseLazyInitCustomElement(e, parserContext);
            }
        }
    }
    
    /**
     * Root method for all parsing.
     * 
     * @param element the element to parse.
     * <p>This works in two scoping modes.  If the parent builder is
     * null then this bean is to be inserted into the provided parser context, in this case the parent builder is null.
     * If the parent builder is provided then the scope is limited and the bean definition is returned.</p>
     * @param parserContext current parsing context
     * @param parentBuilder the parent builder (for nested building).
     * @param lazyInit whether this is lazy initialized;
     * @return the bean definition, <em>unless this is for a parent scoped bean</em>
     */
    @Nullable public static BeanDefinition parseCustomElement(@Nullable final Element element, 
            @Nonnull final ParserContext parserContext,
            @Nullable final BeanDefinitionBuilder parentBuilder,
            final boolean lazyInit) {
        if (element == null) {
            return null;
        }
        final AbstractBeanDefinition containingBd;
        if (parentBuilder != null) {
            containingBd = parentBuilder.getRawBeanDefinition();
        } else {
            containingBd = null;
        }

        final BeanDefinition def = parserContext.getDelegate().parseCustomElement(element, containingBd);
        if (lazyInit) {
            def.setLazyInit(true);
        }
        if (null == parentBuilder) {
            return null;
        }
        return def;
    }

    /**
     * Parse an element into a bean definition and set the lazy-init flag.
     * 
     * @param element the element to parse
     * @param parserContext current parsing context
     * 
     * @since 6.0.0
     */
    @Nullable public static void parseLazyInitCustomElement(@Nullable final Element element, 
            @Nonnull final ParserContext parserContext) {
        if (element == null) {
            return;
        }
        parseCustomElement(element, parserContext, null, true);
    }

    /**
     * Parse the provided Element into the provided registry.
     * 
     * @param springBeans the element to parse
     * @param registry the registry to populate
     */
    public static void
            parseNativeElement(@Nonnull final Element springBeans, @Nullable final BeanDefinitionRegistry registry) {
        final XmlBeanDefinitionReader definitionReader = new XmlBeanDefinitionReader(registry);
        definitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        definitionReader.setNamespaceAware(true);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SerializeSupport.writeNode(springBeans, outputStream);
        definitionReader.loadBeanDefinitions(new InputSource(new ByteArrayInputStream(outputStream.toByteArray())));
    }

    /**
     * Creates a Spring bean factory from the supplied Spring beans element.
     * 
     * @param springBeans to create bean factory from
     * 
     * @return bean factory
     */
    @Nonnull public static BeanFactory createBeanFactory(@Nonnull final Element springBeans) {

        // Pull in the closest xsi:schemaLocation attribute we can find.
        if (!springBeans.hasAttributeNS(XMLConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getNamespaceURI(),
                XMLConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getLocalPart())) {
            Node parent = springBeans.getParentNode();
            while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                final String schemaLoc =
                        ((Element) parent).getAttributeNS(
                                XMLConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getNamespaceURI(),
                                XMLConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getLocalPart());
                if (!Strings.isNullOrEmpty(schemaLoc)) {
                    springBeans.setAttributeNS(XMLConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getNamespaceURI(),
                            XMLConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getPrefix() + ':'
                                    + XMLConstants.XSI_SCHEMA_LOCATION_ATTRIB_NAME.getLocalPart(), schemaLoc);
                    break;
                }
                
                parent = parent.getParentNode();
            }
        }

        final GenericApplicationContext ctx = new FilesystemGenericApplicationContext();
        parseNativeElement(springBeans, ctx);
        ctx.refresh();
        return ctx.getBeanFactory();
    }

    /**
     * Retrieves the bean of the supplied type from the supplied bean factory. Returns null if no bean definition is
     * found.
     * 
     * @param <T> type of bean to return
     * @param beanFactory to get the bean from
     * @param clazz type of the bean to retrieve
     * 
     * @return spring bean
     */
    @Nullable public static <T> T getBean(@Nonnull final BeanFactory beanFactory, @Nonnull final Class<T> clazz) {
        T bean = null;
        try {
            bean = beanFactory.getBean(clazz);
            LOG.debug("created spring bean {}", bean);
        } catch (final NoSuchBeanDefinitionException e) {
            LOG.debug("no spring bean configured of type {}", clazz);
        }
        return bean;
    }

    /**
     * Gets the value of a list-type attribute as a {@link BeanDefinitionBuilder}.
     * 
     * @param attribute attribute whose value will be turned into a list
     * @return a bean which will generate a list of the values.
     */
    @Nonnull public static AbstractBeanDefinition getAttributeValueAsList(@Nonnull final Attr attribute) {
        final BeanDefinitionBuilder result =
                BeanDefinitionBuilder.rootBeanDefinition(StringSupport.class, "stringToList");
        result.addConstructorArgValue(attribute.getValue());
        result.addConstructorArgValue(XMLConstants.LIST_DELIMITERS);
        return result.getBeanDefinition();
    }
    
    /**
     * Gets the value of a boolean-type string as a (ptentially null) {@link Boolean}.
     * 
     * @param string value will be turned into a boolean (or null) (after property replacement)
     * @return a bean which will generate a list of the values.
     */
    @Nullable public static AbstractBeanDefinition getStringValueAsBoolean(@Nullable final String string) {
        final BeanDefinitionBuilder result =
                BeanDefinitionBuilder.rootBeanDefinition(StringSupport.class, "booleanOf");
        result.addConstructorArgValue(string);
        return result.getBeanDefinition();
    }


    /**
     * Gets the text content of a list of {@link Element}s as a {@link ManagedList}.
     * 
     * @param elements the elements whose values will be turned into a list
     * 
     * @return list of values, never null
     */
    @Nonnull public static ManagedList<String> getElementTextContentAsManagedList(
            @Nullable final Collection<Element> elements) {
        
        if (null == elements || elements.isEmpty()) {
            return new ManagedList<>(0);
        }
        
        final ManagedList<String> result = new ManagedList<>(elements.size());
        
        for (final Element element : elements) {
            final String textContent = StringSupport.trimOrNull(element.getTextContent());
            if (null != textContent) {
                result.add(textContent);
            }
        }
        return result;
    }

    /** Return the {@link LanguageRange} associated with this request prepended with
     * the Spring preferred locale.  This allows external (non browser) control of
     * the language.
     * @param request the request to process
     * @return The range returned from {@link HttpServletSupport#getLanguageRange(HttpServletRequest)}
     * with the Locale from {@link RequestContextUtils#getLocale(HttpServletRequest)} prepended
     */
    @Nonnull @NonnullElements @Unmodifiable
    public static List<LanguageRange> getLanguageRange(final HttpServletRequest request) {
        final List<LanguageRange> fromBrowser = HttpServletSupport.getLanguageRange(request);
        final List<LanguageRange> outList = new ArrayList<>(1+fromBrowser.size());

        outList.add(new LanguageRange(RequestContextUtils.getLocale(request).getLanguage()));
        outList.addAll(fromBrowser);
        return List.copyOf(outList);        
    }
}