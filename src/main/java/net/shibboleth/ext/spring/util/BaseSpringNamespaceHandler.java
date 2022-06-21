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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;
import net.shibboleth.utilities.java.support.xml.QNameSupport;

/**
 * A base class for {@link NamespaceHandler} implementations.
 * 
 * <p>This code is heavily based on Spring's <code>NamespaceHandlerSupport</code>. The largest difference is that bean
 * definition parsers may be registered against either an elements name or schema type. During parser lookup the schema
 * type is preferred.</p>
 * 
 * <p>This code also now supports a notion of one or more "secondary" handlers that can be registered to supplement or
 * override the mappings in the main subclass.</p> 
 */
public abstract class BaseSpringNamespaceHandler implements NamespaceHandler {

    /**
     * The base location to look for the secondary mapping files. Can be present in multiple JAR files.
     */
    @Nonnull @NotEmpty
    public static final String DEFAULT_SECONDARY_HANDLER_BASE_LOCATION = "META-INF/net/shibboleth/spring/handlers/";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseSpringNamespaceHandler.class);

    /** Qualifier to include in resource path when looking for secondary handlers. */
    @Nullable @NotEmpty private final String secondaryHandlerQualifier;
    
    /**
     * Stores the {@link BeanDefinitionParser} implementations keyed by the local name of the {@link Element Elements}
     * they handle.
     */
    @Nonnull @NonnullElements private final Map<QName,BeanDefinitionParser> parsers;

    /** Constructor. */
    public BaseSpringNamespaceHandler() {
        this(null);
    }
    
    /**
     * Constructor.
     *
     * @param qualifier qualifier added to secondary handler resource path
     * 
     * @since 7.0.0
     */
    public BaseSpringNamespaceHandler(@Nullable @NotEmpty @ParameterName(name="qualifier") final String qualifier) {
        parsers = new HashMap<>();
        secondaryHandlerQualifier = StringSupport.trimOrNull(qualifier);
    }

    /** {@inheritDoc} */
    public void init() {
        doInit();
        initSecondaryHandlers();
    }

    /**
     * A no-op decorator, returns the input.
     * 
     * @param node the node decorating a the given bean definition
     * @param definition the bean being decorated
     * @param parserContext the current parser context
     * 
     * @return the input bean definition
     */
    @Override public BeanDefinitionHolder decorate(final Node node, final BeanDefinitionHolder definition, 
            final ParserContext parserContext) {
        return definition;
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
    @Override public BeanDefinition parse(final Element element, final ParserContext parserContext) {
        return findParserForElement(element).parse(element, parserContext);
    }

    /**
     * Subclasses should override this method to allow for installation of {@link SecondaryNamespaceHandler}
     * instances.
     * 
     * @since 7.0.0
     */
    protected void doInit() {
        
    }
        
    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using the local name of the supplied
     * {@link Element}.
     * 
     * @param element the element to locate the bean definition parser for
     * 
     * @return the parser for the given bean element
     */
    protected BeanDefinitionParser findParserForElement(final Element element) {
        BeanDefinitionParser parser = null;

        final QName typeName = DOMTypeSupport.getXSIType(element);
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
     * Subclasses can call this to register the supplied {@link BeanDefinitionParser} to handle the specified element.
     * The element name is the local (non-namespace qualified) name.
     * 
     * @param elementNameOrType the element name or schema type the parser is for
     * @param parser the parser to register
     */
    protected void registerBeanDefinitionParser(final QName elementNameOrType, final BeanDefinitionParser parser) {
        parsers.put(elementNameOrType, parser);
    }

    /**
     * Initializes any registered secondary handlers.
     * 
     * <p>Subclasses that allow for this feature should override {@link #doInit()}, while existing/legacy
     * handlers without this feature may continue to override {@link #init()}.</p>
     */
    private void initSecondaryHandlers() {
        try {
            final Enumeration<URL> urls = ClassLoader.getSystemResources(
                    DEFAULT_SECONDARY_HANDLER_BASE_LOCATION + secondaryHandlerQualifier);
            while (urls.hasMoreElements()) {
                final URL url = urls.nextElement();
                final URLConnection con = url.openConnection();
                try (final InputStream is = con.getInputStream();
                     final InputStreamReader isr = new InputStreamReader(is);
                     final BufferedReader br = new BufferedReader(isr)) {
                     final String className = br.readLine();
                     @SuppressWarnings("unchecked")
                     final Class<SecondaryNamespaceHandler> clazz =
                             (Class<SecondaryNamespaceHandler>) Class.forName(className);
                     final SecondaryNamespaceHandler namespaceHandler = BeanUtils.instantiateClass(clazz);
                     namespaceHandler.init(parsers);
                }
            }
        } catch (final IOException|ClassNotFoundException|ClassCastException|BeanInstantiationException  e) {
            log.error("Secondary initialization failed for namespace {}", secondaryHandlerQualifier, e);
            throw new BeanCreationException("Secondary initialization failed for namespace '"
                    + secondaryHandlerQualifier + "'", e);
        }
    }

}