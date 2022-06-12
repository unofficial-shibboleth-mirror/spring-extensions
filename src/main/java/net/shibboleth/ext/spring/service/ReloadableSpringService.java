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

package net.shibboleth.ext.spring.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.AbstractReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;


/**
 * This class provides a reloading interface to a {@link ServiceableComponent} via Spring.
 * This class extends {@link org.springframework.context.Lifecycle}. and thus 
 * it acts as the bridge between this interface and
 * {@link net.shibboleth.utilities.java.support.component.InitializableComponent} and
 * {@link net.shibboleth.utilities.java.support.component.DestructableComponent}

 * 
 * @param <T> The precise service being implemented.
 */
@ThreadSafe
public class ReloadableSpringService<T> extends AbstractReloadableService<T> implements ApplicationContextAware,
        BeanNameAware, Lifecycle {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReloadableSpringService.class);

    /** List of configuration resources for this service. */
    @Nonnull @NonnullElements private List<Resource> serviceConfigurations;

    /** List of bean factory post processors for this service's content. */
    @Nonnull @NonnullElements private List<BeanFactoryPostProcessor> factoryPostProcessors;

    /** List of bean post processors for this service's content. */
    @Nonnull @NonnullElements private List<BeanPostProcessor> postProcessors;
    
    /** Bean profiles to enable. */
    @Nonnull @NonnullElements private Collection<String> beanProfiles;

    /** Conversion service to use. */
    @Nullable private ConversionService conversionService;
    
    /** The class we are looking for. */
    @Nonnull private final Class<T> theClaz;

    /** How to summon up the {@link ServiceableComponent} from the {@link ApplicationContext}. */
    @Nonnull private final Function<ApplicationContext, ServiceableComponent<T>> serviceStrategy;

    /** Application context owning this engine. */
    @Nullable private ApplicationContext parentContext;

    /** The bean name. */
    @Nullable private String beanName;

    /** The last known good component. */
    @Nullable private ServiceableComponent<T> cachedComponent;

    /** Did the last load fail? An optimization only. */
    private boolean lastLoadFailed = true;

    /**
     * Time when the service configuration for the given index was last observed to have changed.
     * A null indicates the configuration resource did not exist.
     */
    @Nullable private Instant[] resourceLastModifiedTimes;

    /**
     * Constructor.
     * 
     * @param claz The interface being implemented.
     */
    public ReloadableSpringService(@Nonnull @ParameterName(name="claz") final Class<T> claz) {
        this(claz, new ClassBasedServiceStrategy<T>());
    }

    /**
     * Constructor.
     * 
     * @param claz The interface being implemented.
     * @param strategy the strategy to use to look up servicable component to look for.
     */
    public ReloadableSpringService(@Nonnull @ParameterName(name="claz") final Class<T> claz,
             @Nonnull @ParameterName(name="strategy")
                final Function<ApplicationContext,ServiceableComponent<T>> strategy) {
        theClaz = Constraint.isNotNull(claz, "Class cannot be null");
        serviceStrategy = Constraint.isNotNull(strategy, "Strategy cannot be null");
        factoryPostProcessors = Collections.emptyList();
        postProcessors = Collections.emptyList();
        beanProfiles = Collections.emptyList();
        serviceConfigurations = Collections.emptyList();
    }

    /**
     * Gets the application context that is the parent to this service's context.
     * 
     * @return application context that is the parent to this service's context
     */
    @Nullable public ApplicationContext getParentContext() {
        return parentContext;
    }

    /**
     * Sets the application context that is the parent to this service's context.
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param context context that is the parent to this service's context, may be null
     */
    public void setParentContext(@Nullable final ApplicationContext context) {
        throwSetterPreconditionExceptions();

        parentContext = context;
    }

    /**
     * Gets an unmodifiable list of configurations for this service.
     * 
     * @return unmodifiable list of configurations for this service
     */
    @Nonnull public List<Resource> getServiceConfigurations() {
        return serviceConfigurations;
    }

    /**
     * Set the list of configurations for this service.
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param configs list of configurations for this service
     */
    public void setServiceConfigurations(@Nonnull @NonnullElements final List<Resource> configs) {
        throwSetterPreconditionExceptions();

        serviceConfigurations = List.copyOf(Constraint.isNotNull(configs, "Service configurations cannot be null"));
        if (!serviceConfigurations.isEmpty()) {
            resourceLastModifiedTimes = new Instant[serviceConfigurations.size()];

            final int numOfResources = serviceConfigurations.size();
            Resource serviceConfig;
            for (int i = 0; i < numOfResources; i++) {
                serviceConfig = serviceConfigurations.get(i);
                try {
                    if (serviceConfig.exists()) {
                        resourceLastModifiedTimes[i] = Instant.ofEpochMilli(serviceConfig.lastModified());
                    } else {
                        resourceLastModifiedTimes[i] = null;
                    }
                } catch (final IOException e) {
                    log.info("{} Configuration resource '" + serviceConfig.getDescription()
                            + "' last modification date could not be determined", getLogPrefix(), e);
                    resourceLastModifiedTimes[i] = null;
                }
            }
        } else {
            resourceLastModifiedTimes = null;
        }
    }
    
    /**
     * Set the strategy by which the Service can locate the resources it needs to know about.
     *
     * <p>Not implemented.</p>
     *
     * @param strategy the way to get the resources.  Precise details are tbd.
     */
    public void setServiceConfigurationStrategy(@Nonnull final Function<?, List<Resource>> strategy) {
        throwSetterPreconditionExceptions();
        throw new UnsupportedOperationException("This UnsupportedOperationException method has not been implemented");
    }

    /**
     * Set the list of bean factory post processors for this service.
     * 
     * @param processors bean factory post processors to apply
     */
    public void setBeanFactoryPostProcessors(
            @Nonnull @NonnullElements final List<BeanFactoryPostProcessor> processors) {
        throwSetterPreconditionExceptions();
        Constraint.isNotNull(processors, "BeanFactoryPostProcessor collection cannot be null");

        factoryPostProcessors = List.copyOf(processors);
    }

    /**
     * Set the list of bean post processors for this service.
     * 
     * @param processors bean post processors to apply
     */
    public void setBeanPostProcessors(@Nonnull @NonnullElements final List<BeanPostProcessor> processors) {
        throwSetterPreconditionExceptions();
        Constraint.isNotNull(processors, "BeanPostProcessor collection cannot be null");

        postProcessors = List.copyOf(processors);
    }
    
    /**
     * Set the bean profiles for this service.
     * 
     * @param profiles bean profiles to apply
     * 
     * @since 5.4.0
     */
    public void setBeanProfiles(@Nonnull @NonnullElements final Collection<String> profiles) {
        throwSetterPreconditionExceptions();
        
        beanProfiles = StringSupport.normalizeStringCollection(profiles);
    }
    
    /**
     * Set a conversion service to use.
     * 
     * @param service conversion service
     * 
     * @since 5.4.0
     */
    public void setConversionService(@Nullable final ConversionService service) {
        throwSetterPreconditionExceptions();

        conversionService = service;
    }

    /** {@inheritDoc} */
    @Override
    public final void start() {
        try {
            initialize();
        } catch (final ComponentInitializationException e) {
            throw new BeanInitializationException("Could not start service", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void stop() {
        destroy();
    }

    /** {@inheritDoc}. */
    @Override
    public boolean isRunning() {
        return isInitialized() && !isDestroyed();
    }


// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override protected boolean shouldReload() {
        // Loop over each resource and check if the any resources have been changed since
        // the last time the service was reloaded. I believe a read lock is all we need here
        // to allow use of the service to proceed while we check on the state. Actual reloading
        // requires the write lock, and the only post-initialization code that reads or writes
        // the array of resource mod-time data is this code, which is on one thread.

        if (resourceLastModifiedTimes == null) {
            return false;
        }

        if (lastLoadFailed) {
            return true;
        }

        boolean configResourceChanged = false;
        final int numOfResources = serviceConfigurations.size();

        Resource serviceConfig;
        Instant serviceConfigLastModified;
        for (int i = 0; i < numOfResources; i++) {
            serviceConfig = serviceConfigurations.get(i);
            try {
                if (resourceLastModifiedTimes[i] == null && !serviceConfig.exists()) {
                    // Resource did not exist and still does not exist.
                    log.debug("{} Resource remains unavailable/inaccessible: '{}'", getLogPrefix(),
                            serviceConfig.getDescription());
                } else if (resourceLastModifiedTimes[i] == null && serviceConfig.exists()) {
                    // Resource did not exist, but does now.
                    log.debug("{} Resource was unavailable, now present: '{}'", getLogPrefix(),
                            serviceConfig.getDescription());
                    configResourceChanged = true;
                    resourceLastModifiedTimes[i] = Instant.ofEpochMilli(serviceConfig.lastModified());
                } else if (resourceLastModifiedTimes[i] != null && !serviceConfig.exists()) {
                    // Resource existed, but is now unavailable.
                    log.debug("{} Resource was available, now is not: '{}'", getLogPrefix(),
                            serviceConfig.getDescription());
                    configResourceChanged = true;
                    resourceLastModifiedTimes[i] = null;
                } else {
                    // Check to see if an existing resource, that still exists, has been modified.
                    serviceConfigLastModified = Instant.ofEpochMilli(serviceConfig.lastModified());
                    if (!serviceConfigLastModified.equals(resourceLastModifiedTimes[i])) {
                        log.debug("{} Resource has changed: '{}'", getLogPrefix(), serviceConfig.getDescription());
                        configResourceChanged = true;
                        resourceLastModifiedTimes[i] = serviceConfigLastModified;
                    } else {
                        log.trace("{} Resource has not changed '{}'", getLogPrefix(), serviceConfig.getDescription());
                    }
                }
            } catch (final IOException e) {
                log.info("{} Configuration resource '{}' last modification date could not be determined",
                        getLogPrefix(), serviceConfig.getDescription(), e);
                configResourceChanged = true;
            }
        }

        return configResourceChanged;
    }
// Checkstyle: CyclomaticComplexity ON

    /** {@inheritDoc} */
    @Override protected void doReload() {
        super.doReload();

        log.debug("{} Creating new ApplicationContext for service '{}'", getLogPrefix(), getId());
        log.debug("{} Reloading from {}", getLogPrefix(), getServiceConfigurations());
        final GenericApplicationContext appContext;
        try {
            appContext = new ApplicationContextBuilder()
                .setName(getId()).setParentContext(getParentContext())
                .setServiceConfigurations(getServiceConfigurations())
                .setBeanFactoryPostProcessors(factoryPostProcessors)
                .setBeanPostProcessors(postProcessors)
                .setBeanProfiles(beanProfiles)
                .setConversionService(conversionService)
                .build();
        } catch (final FatalBeanException e) {
            throw new ServiceException(e);
        }

        log.debug("{} New Application Context created for service '{}'", getLogPrefix(), getId());

        final ServiceableComponent<T> service;
        try {
            service = serviceStrategy.apply(appContext);
        } catch (final Exception e) {
            appContext.close();
            throw new ServiceException("Failed to load " + getServiceConfigurations(), e);
        }

        service.pinComponent();

        // Now check it's the right type before we continue.
        final T theComponent = service.getComponent();

        log.debug("{} Testing that {} is a superclass of {}", getLogPrefix(), theComponent.getClass(), theClaz);

        if (!theClaz.isAssignableFrom(theComponent.getClass())) {
            //
            // tear it down
            //
            service.unpinComponent();
            service.unloadComponent();
            throw new ServiceException("Class was not the same or a superclass of configured class");
        }

        // Otherwise we are ready to swap in the new component; so only
        // now do we grab the lock.
        //
        // Note that we are grabbing the lock on the component before the lock on this
        // object, which would be an inversion with the getServiceableComponent ranking
        // except the component will never be seen before we drop the lock and so
        // there can be no inversion
        //
        final ServiceableComponent<T> oldComponent;
        synchronized (this) {
            oldComponent = cachedComponent;
            cachedComponent = service;
            service.unpinComponent();
        }
        
        log.info("{} Completed reload and swapped in latest configuration for service '{}'", getLogPrefix(), getId());
        
        if (null != oldComponent) {
            log.debug("{} Unloading previous configuration for service '{}'", getLogPrefix(), getId());
            oldComponent.unloadComponent();
        }
        lastLoadFailed = false;
        log.info("{} Reload complete", getLogPrefix());
    }

    /** {@inheritDoc} */
    @Override protected void doDestroy() {
        final ServiceableComponent<T> oldComponent = cachedComponent;
        cachedComponent = null;
        // And tear down. Note that we are synchronized on this right now
        // and this will grab the lock - but that is OK because the ranking
        // is to lock this object, then the ServicableComponent.
        if (null != oldComponent) {
            oldComponent.unloadComponent();
        }
        super.doDestroy();
    }

    /**
     * Get the serviceable component. We do this under interlock and grab the lock on the component.
     * 
     * @return the <em>pinned</em> component.
     */
    @Override public synchronized ServiceableComponent<T> getServiceableComponent() {
        if (null == cachedComponent) {
            return null;
        }
        cachedComponent.pinComponent();
        return cachedComponent;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(final ApplicationContext applicationContext) {
        setParentContext(applicationContext);
    }

    /** {@inheritDoc} */
    public void setBeanName(final String name) {
        beanName = name;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {        
        if (getId() == null && beanName != null) {
            setId(beanName);
        }
        
        super.doInitialize();
    }
    
}