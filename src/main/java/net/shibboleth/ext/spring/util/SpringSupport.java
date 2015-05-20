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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringBooleanToPredicateConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import net.shibboleth.utilities.java.support.xml.XMLConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.google.common.base.Strings;

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

// Checkstyle: ParameterNumber OFF
    /**
     * Creates a new, started, application context from the given configuration resources.
     * 
     * @param name name of the application context
     * @param configurationResources configuration resources
     * @param factoryPostProcessors post processors to inject
     * @param postProcessors post processors to inject
     * @param initializers application context initializers
     * @param parentContext parent context, or null if there is no parent
     * 
     * @return the created context
     */
    @Nonnull public static GenericApplicationContext newContext(@Nonnull @NotEmpty final String name,
            @Nonnull @NonnullElements final List<Resource> configurationResources,
            @Nonnull @NonnullElements final List<BeanFactoryPostProcessor> factoryPostProcessors,
            @Nonnull @NonnullElements final List<BeanPostProcessor> postProcessors,
            @Nonnull @NonnullElements final List<ApplicationContextInitializer> initializers,
            @Nullable final ApplicationContext parentContext) {
        
        final GenericApplicationContext context = new FilesystemGenericApplicationContext(parentContext);
        context.setDisplayName("ApplicationContext:" + name);
        
        context.setResourceLoader(new PreferFileSystemResourceLoader());

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(new HashSet<>(Arrays.asList(new DurationToLongConverter(), new StringToIPRangeConverter(),
                new StringBooleanToPredicateConverter())));
        service.afterPropertiesSet();

        for (final BeanFactoryPostProcessor bfpp : factoryPostProcessors) {
            context.addBeanFactoryPostProcessor(bfpp);
        }

        context.getBeanFactory().setConversionService(service.getObject());
        for (final BeanPostProcessor bpp : postProcessors) {
            context.getBeanFactory().addBeanPostProcessor(bpp);
        }

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(configurationResources.toArray(new Resource[] {}));

        if (initializers != null) {
            for (ApplicationContextInitializer initializer : initializers) {
                initializer.initialize(context);
            }
        }

        context.refresh();
        return context;
    }
// Checkstyle: ParameterNumber ON


    /**
     * Parse list of elements into bean definitions.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * 
     * @return list of bean definitions
     */
    // TODO better javadoc, annotations
    @Nullable public static ManagedList<BeanDefinition> parseCustomElements(
            @Nullable @NonnullElements final Collection<Element> elements, @Nonnull final ParserContext parserContext) {
        if (elements == null) {
            return null;
        }

        ManagedList<BeanDefinition> definitions = new ManagedList<>(elements.size());
        for (Element e : elements) {
            if (e != null) {
                definitions.add(parseCustomElement(e, parserContext));
            }
        }

        return definitions;
    }
    
    /**
     * Parse an element into a bean definition.
     * 
     * @param element the element to parse
     * @param parserContext current parsing context
     * 
     * @return the parsed bean definition
     */
    @Nullable public static BeanDefinition parseCustomElement(@Nullable final Element element, 
            @Nonnull final ParserContext parserContext) {
        if (element == null) {
            return null;
        }

        return parserContext.getDelegate().parseCustomElement(element);
    }

    /**
     * Parse the provided Element into the provided registry.
     * 
     * @param springBeans the element to parse
     * @param registry the registry to populate
     */
    public static void
            parseNativeElement(@Nonnull final Element springBeans, @Nullable BeanDefinitionRegistry registry) {
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
                } else {
                    parent = parent.getParentNode();
                }
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
        } catch (NoSuchBeanDefinitionException e) {
            LOG.debug("no spring bean configured of type {}", clazz);
        }
        return bean;
    }

    /**
     * Gets the value of a list-type attribute as a {@link ManagedList}.
     * 
     * @param attribute attribute whose value will be turned into a list
     * 
     * @return list of values, never null
     */
    @Nonnull public static ManagedList<String> getAttributeValueAsManagedList(@Nullable final Attr attribute) {
        List<String> valuesAsList = AttributeSupport.getAttributeValueAsList(attribute);
        final ManagedList<String> managedList = new ManagedList<>(valuesAsList.size());
        managedList.addAll(valuesAsList);
        return managedList;
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

}