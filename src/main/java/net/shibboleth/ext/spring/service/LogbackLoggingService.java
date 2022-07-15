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
import java.io.InputStream;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.io.Closeables;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.service.AbstractReloadableService;
import net.shibboleth.utilities.java.support.service.LoggingService;
import net.shibboleth.utilities.java.support.service.ServiceException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Simple {@link LoggingService} that watches for logback configuration file changes
 * and reloads the file when a change occurs.
 * 
 * @since 7.0.0
 */
public class LogbackLoggingService extends AbstractReloadableService<Object>
        implements LoggingService, ApplicationContextAware {
    
    /** Logback logger context. */
    @NonnullAfterInit private LoggerContext loggerContext;

    /** Logger used to log messages without relying on the logging system to be full initialized. */
    @NonnullAfterInit private StatusManager statusManager;

    /** URL to the fallback logback configuration found in the IdP jar. */
    @NonnullAfterInit private Resource fallbackConfiguration;

    /** Logging configuration resource. */
    @NonnullAfterInit private Resource configurationResource;
    
    /** Optional name of a property to populate into the {@link LoggerContext}. */
    @Nullable @NotEmpty private String homePropertyName;

    /** Spring application context. */
    @Nullable private ApplicationContext applicationContext;

    /** {@inheritDoc} */
    public void setLoggingConfiguration(@Nonnull final Resource configuration) {
        checkSetterPreconditions();
        configurationResource = Constraint.isNotNull(configuration, "Logging configuration resource cannot be null");
    }
    
    /**
     * Set a resource to use as a fallback configuration if the primary is unavailable.
     * 
     * @param fallback fallback configuration resouurce
     */
    public void setFallbackConfiguration(@Nonnull final Resource fallback) {
        checkSetterPreconditions();
        fallbackConfiguration = Constraint.isNotNull(fallback, "Logging configuration falback resource cannot be null");
    }
    
    /**
     * Set name of a property to load into {@link LoggerContext}.
     * 
     * @param name property name
     */
    public void setHomePropertyName(@Nullable @NotEmpty final String name) {
        checkSetterPreconditions();
        homePropertyName = StringSupport.trimOrNull(name);
    }

    /** {@inheritDoc} */
    public void setApplicationContext(final ApplicationContext context) {
        applicationContext = context;
    }

    /**
     * {@inheritDoc}.
     * 
     * This service does not support a ServiceableComponent, so return null.
     */
    @Override
    @Nullable public ServiceableComponent<Object> getServiceableComponent() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        if (configurationResource == null) {
            throw new ComponentInitializationException("Logging configuration must be specified.");
        }
    
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        statusManager = loggerContext.getStatusManager();
        if (!fallbackConfiguration.exists()) {
            if (isFailFast()) {
                throw new ComponentInitializationException(getLogPrefix() + "Cannot locate fallback configuration");
            }
            statusManager.add(new ErrorStatus("Cannot locate fallback configuration at "
                    + fallbackConfiguration.getDescription(), this));
        }
        super.doInitialize();
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized boolean shouldReload() {
        try {
            final Instant lastReload = getLastSuccessfulReloadInstant();
            if (null == lastReload) {
                return true;
            }
            return configurationResource.lastModified() > lastReload.toEpochMilli();
        } catch (final IOException e) {
            statusManager.add(new ErrorStatus(
                    "Error checking last modified time of logging service configuration resource "
                            + configurationResource.getDescription(), this, e));
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized void doReload() {
        loadLoggingConfiguration();
    }

    /**
     * Reads and loads in a new logging configuration.
     * 
     * @throws ServiceException thrown if there is a problem loading the logging configuration
     */
    protected void loadLoggingConfiguration() {
        InputStream ins = null;
        try {
            statusManager.add(new InfoStatus("Loading new logging configuration resource: "
                    + configurationResource.getDescription(), this));
            ins = configurationResource.getInputStream();
            loadLoggingConfiguration(ins);
        } catch (final Exception e) {
            try {
                Closeables.close(ins, true);
            } catch (final IOException e1) {
                // swallowed && logged by Closeables but...
                throw new ServiceException(e1);
            }
            statusManager.add(new ErrorStatus("Error loading logging configuration file: "
                    + configurationResource.getDescription(), this, e));
            try {
                statusManager.add(new InfoStatus("Loading fallback logging configuration", this));
                ins = fallbackConfiguration.getInputStream();
                loadLoggingConfiguration(ins);
            } catch (final IOException ioe) {
                try {
                    Closeables.close(ins, true);
                } catch (final IOException e1) {
                    // swallowed && logged by Closeables
                    throw new ServiceException(e1);
                }
                statusManager.add(new ErrorStatus("Error loading fallback logging configuration", this, e));
                throw new ServiceException("Unable to load fallback logging configuration");
            }
        } finally {
            try {
                Closeables.close(ins, true);
            } catch (final IOException e) {
                // swallowed && logged by Closeables
                throw new ServiceException(e);
            }
        }
    }

    /**
     * Loads a logging configuration in to the active logger context. Error messages are printed out to the status
     * manager.
     * 
     * @param loggingConfig logging configuration file
     * 
     * @throws ServiceException thrown is there is a problem loading the logging configuration
     */
    protected void loadLoggingConfiguration(final InputStream loggingConfig) {
        try {
            loggerContext.reset();
            loadHomeProperty();
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(loggingConfig);
            loggerContext.start();
        } catch (final JoranException e) {
            throw new ServiceException(e);
        }
    }

    /**
     * Add the "home" property from the Spring application context to the logger context.
     * 
     * <p>This is principally to allow a defaulted value of this property to be referenced within
     * the logging configuration itself as a location.</p>
     */
    protected void loadHomeProperty() {
        if (applicationContext != null && homePropertyName != null) {
            final String home = applicationContext.getEnvironment().getProperty(homePropertyName);
            if (home != null) {
                statusManager.add(
                        new InfoStatus("Setting logger property '" + homePropertyName + "' to '" + home + "'", this));
                loggerContext.putProperty(homePropertyName, home);
            }
        }
    }
    
}