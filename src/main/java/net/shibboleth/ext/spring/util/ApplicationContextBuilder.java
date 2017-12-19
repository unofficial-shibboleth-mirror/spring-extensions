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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.ext.spring.config.BooleanToPredicateConverter;
import net.shibboleth.ext.spring.config.DurationToLongConverter;
import net.shibboleth.ext.spring.config.StringBooleanToPredicateConverter;
import net.shibboleth.ext.spring.config.StringToIPRangeConverter;
import net.shibboleth.ext.spring.config.StringToResourceConverter;
import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.resource.ConditionalResourceResolver;
import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * Fluent builder for a {@link FilesystemGenericApplicationContext} equipped with various standard features,
 * behavior, converters, etc.
 * 
 * @since 5.4.0
 */
public class ApplicationContextBuilder {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ApplicationContextBuilder.class);

    /** Context name. */
    @Nullable @NotEmpty private String contextName;
    
    /** List of configuration resources for this service. */
    @Nullable @NonnullElements private List<Resource> configurationResources;

    /** Conversion service to use. */
    @Nullable private ConversionService conversionService;
    
    /** List of context initializers. */
    @Nullable @NonnullElements private List<ApplicationContextInitializer<? super FilesystemGenericApplicationContext>>
    contextInitializers;

    /** List of bean factory post processors for this service's content. */
    @Nullable @NonnullElements private List<BeanFactoryPostProcessor> factoryPostProcessors;

    /** List of bean post processors for this service's content. */
    @Nullable @NonnullElements private List<BeanPostProcessor> postProcessors;
    
    /** Bean profiles to enable. */
    @Nullable @NonnullElements private Collection<String> beanProfiles;

    /** Application context owning this engine. */
    @Nullable private ApplicationContext parentContext;
    
    /**
     * Set the name of the context.
     * 
     * @param name name
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setName(@Nullable @NotEmpty final String name) {
        contextName = StringSupport.trimOrNull(name);
        
        return this;
    }

    /**
     * Set a conversion service to use.
     * 
     * @param service conversion service
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setConversionService(@Nullable final ConversionService service) {
        conversionService = service;
        
        return this;
    }
    
    /**
     * Set a single configuration resource for this context.
     * 
     * @param config configuration for this context
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setServiceConfiguration(@Nonnull final Resource config) {
        configurationResources = Collections.singletonList(Constraint.isNotNull(config, "Resource cannot be null"));
        
        return this;
    }
    
    /**
     * Set the list of configurations for this context.
     * 
     * @param configs list of configurations for this context
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setServiceConfigurations(
            @Nonnull @NonnullElements final List<Resource> configs) {
        configurationResources = new ArrayList<>(Collections2.filter(configs, Predicates.notNull()));
        
        return this;
    }

    /**
     * Set a single context initializer for this context.
     * 
     * @param initializer initializer to apply
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setContextInitializer(
            @Nonnull final ApplicationContextInitializer<? super FilesystemGenericApplicationContext> initializer) {
        Constraint.isNotNull(initializer, "ApplicationContextInitializer cannot be null");
        
        contextInitializers =
                Collections.<ApplicationContextInitializer<? super FilesystemGenericApplicationContext>>singletonList(
                        initializer);
        
        return this;
    }
    
    /**
     * Set the list of context initializers for this context.
     * 
     * @param initializers initializers to apply
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setContextInitializers(
            @Nonnull @NonnullElements
            final List<ApplicationContextInitializer<? super FilesystemGenericApplicationContext>> initializers) {
        contextInitializers = new ArrayList<>(Collections2.filter(initializers, Predicates.notNull()));
        
        return this;
    }
    
    /**
     * Set a single bean factory post processor for this context.
     * 
     * @param processor bean factory post processor to apply
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setBeanFactoryPostProcessor(
            @Nonnull final BeanFactoryPostProcessor processor) {
        Constraint.isNotNull(processor, "BeanFactoryPostProcessor cannot be null");
        
        factoryPostProcessors = Collections.singletonList(processor);
        
        return this;
    }


    /**
     * Set the list of bean factory post processors for this context.
     * 
     * @param processors bean factory post processors to apply
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setBeanFactoryPostProcessors(
            @Nonnull @NonnullElements final List<BeanFactoryPostProcessor> processors) {
        factoryPostProcessors = new ArrayList<>(Collections2.filter(processors, Predicates.notNull()));
        
        return this;
    }
    
    /**
     * Set a single bean post processor for this context.
     * 
     * @param processor bean post processor to apply
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setBeanPostProcessor(@Nonnull final BeanPostProcessor processor) {
        Constraint.isNotNull(processor, "BeanPostProcessor cannot be null");
        
        postProcessors = Collections.singletonList(processor);
        
        return this;
    }

    /**
     * Set the list of bean post processors for this context.
     * 
     * @param processors bean post processors to apply
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setBeanPostProcessors(
            @Nonnull @NonnullElements final List<BeanPostProcessor> processors) {
        postProcessors = new ArrayList<>(Collections2.filter(processors, Predicates.notNull()));
        
        return this;
    }
    
    /**
     * Set the bean profiles for this context.
     * 
     * @param profiles bean profiles to apply
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setBeanProfiles(
            @Nonnull @NonnullElements final Collection<String> profiles) {
        beanProfiles = StringSupport.normalizeStringCollection(profiles);
        
        return this;
    }
    
    /**
     * Set a custom {@link ConversionService} to use.
     * 
     * @param context parent context
     * 
     * @return this builder
     */
    @Nonnull public ApplicationContextBuilder setParentContext(@Nullable final ApplicationContext context) {
        parentContext = context;
        
        return this;
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /**
     * Build the context.
     * 
     * @return the built context, initialized and loaded
     */
    @Nonnull public GenericApplicationContext build() {
        
        final GenericApplicationContext context = new FilesystemGenericApplicationContext(parentContext);
        context.setDisplayName("ApplicationContext:" + contextName != null ? contextName : "anonymous");
        
        context.setResourceLoader(new PreferFileSystemResourceLoader());
        context.addProtocolResolver(new ConditionalResourceResolver());
        
        if (conversionService != null) {
            context.getBeanFactory().setConversionService(conversionService);
        } else {
            final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
            service.setConverters(new HashSet<>(Arrays.asList(
                    new DurationToLongConverter(),
                    new StringToIPRangeConverter(),
                    new BooleanToPredicateConverter(),
                    new StringBooleanToPredicateConverter(),
                    new StringToResourceConverter())));
            service.afterPropertiesSet();
            context.getBeanFactory().setConversionService(service.getObject());
        }
        
        if (factoryPostProcessors != null) {
            for (final BeanFactoryPostProcessor bfpp : factoryPostProcessors) {
                context.addBeanFactoryPostProcessor(bfpp);
            }
        }

        if (postProcessors != null) {
            for (final BeanPostProcessor bpp : postProcessors) {
                context.getBeanFactory().addBeanPostProcessor(bpp);
            }
        }
        
        if (beanProfiles != null) {
            context.getEnvironment().setActiveProfiles(beanProfiles.toArray(new String[0]));
        }

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        if (configurationResources != null) {
            beanDefinitionReader.loadBeanDefinitions(configurationResources.toArray(new Resource[] {}));
        }

        if (contextInitializers != null) {
            for (final ApplicationContextInitializer initializer : contextInitializers) {
                initializer.initialize(context);
            }
        }

        context.refresh();
        return context;
    }
// Checkstyle: CyclomaticComplexity ON
    
}