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

import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Implementation of {@link ServiceableComponent} that does most of the work required. It leverages the spring
 * environment to allow easy cleanup.
 * 
 * @param <T> The type of service.
 */
public abstract class AbstractServiceableComponent<T> extends AbstractIdentifiableInitializableComponent implements
        ServiceableComponent<T>, ApplicationContextAware {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractServiceableComponent.class);

    /** The context used to load this bean. */
    @Nullable private ApplicationContext applicationContext;

    /**
     * Lock for this service. We make it unfair since we will control access and there will only ever be contention
     * during unload.
     */
    @Nonnull private final ReentrantReadWriteLock serviceLock;

    /** Constructor. */
    public AbstractServiceableComponent() {
        serviceLock = new ReentrantReadWriteLock(false);
    }
    
    /** {@inheritDoc} */
    @Override public void setApplicationContext(final ApplicationContext context) {
        checkSetterPreconditions();
        applicationContext = context;
    }

    /**
     * Get the context used to load this bean.
     * 
     * @return the context.
     */
    @Nullable public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * {@inheritDoc}.
     */
    @Override @Nonnull public abstract T getComponent();

    /**
     * {@inheritDoc} Grab the service lock shared. This will block unloads until {@link #unpinComponent()} is called.
     */
    @Override public void pinComponent() {
        serviceLock.readLock().lock();
    }

    /** {@inheritDoc} drop the shared lock. */
    @Override public void unpinComponent() {
        serviceLock.readLock().unlock();
    }

    /** {@inheritDoc}. Grab the service lock ex and then call spring to tear everything down. */
    @Override public void unloadComponent() {
        if (null == applicationContext) {
            log.debug("Component '{}': Component already unloaded", getId());
            return;
        }

        ConfigurableApplicationContext oldContext = null;
        log.debug("Component '{}': Component unload called", getId());
        try {
            log.trace("Component '{}': Queueing for write lock", getId());
            serviceLock.writeLock().lock();
            log.trace("Component '{}': Got write lock", getId());
            oldContext = (ConfigurableApplicationContext) applicationContext;
            applicationContext = null;
        } finally {
            serviceLock.writeLock().unlock();
        }

        if (null != oldContext) {
            log.debug("Component '{}': Closing the appcontext", getId());
            oldContext.close();
        }
        // If we were not created by spring we need to do the destroy of ourself.
        // Note that we will end up being called here but will fall out at the top.
        destroy();
    }

    /**
     * {@inheritDoc}. Force unload; this will usually be a no-op since the component should have been explicitly
     * unloaded, but we do the unload here so that error cases also clean up.
     */
    @Override protected void doDestroy() {
        unloadComponent();
        super.doDestroy();
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (applicationContext == null) {
            throw new ComponentInitializationException(getId()
                    + ": Application context not set");
        }
        if (!(applicationContext instanceof ConfigurableApplicationContext)) {
            throw new ComponentInitializationException(getId()
                    + ": Application context did not implement ConfigurableApplicationContext");
        }
    }

}