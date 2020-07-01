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

package net.shibboleth.ext.spring.cli;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.ResourcePropertySource;

import com.beust.jcommander.JCommander;

import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A simple driver for a Spring-based CLI.
 * 
 * This class handles a single parameters, the primary Spring configuration resource. Additional parameters may be
 * handled by subclasses.
 * 
 * All logging is done in accordance with the logback.xml file included in the library. If you wish to use a
 * different logging configuration you may do so using the <code>-Dlogback.configurationFile=/path/to/logback.xml</code>
 * JVM configuration option.
 * 
 * @param <T> argument object type
 */
public abstract class AbstractCommandLine<T extends CommandLineArguments> {

    /** Name of system property for command line argument class. */
    @Nonnull @NotEmpty public static final String ARGS_PROPERTY = "net.shibboleth.ext.spring.cli.arguments";

    /** Return code indicating command completed successfully, {@value} . */
    public static final int RC_OK = 0;

    /** Return code indicating an initialization error, {@value} . */
    public static final int RC_INIT = 1;

    /** Return code indicating an error reading files, {@value} . */
    public static final int RC_IO = 2;

    /** Return code indicating an unknown error occurred, {@value} . */
    public static final int RC_UNKNOWN = -1;
    
    /** Spring context. */
    @Nullable private GenericApplicationContext applicationContext;
        
    /**
     * Get the Spring context.
     * 
     * @return Spring context
     */
    @Nonnull protected GenericApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("No application context installed");
        }
        return applicationContext;
    }

    /**
     * Run method.
     * 
     * @param args command line arguments
     * 
     * @return exit code
     */
    protected int run(@Nonnull final String[] args) {

        final T argObject;
        
        try {
            final Constructor<T> construct = getArgumentClass().getConstructor();
            argObject = construct.newInstance();
            final JCommander jc = new JCommander(argObject);
            jc.parse(args);
            if (argObject.isHelp()) {
                argObject.printHelp(System.out);
                return RC_OK;
            } else if (argObject.isVersion()) {
                System.out.println(getVersion());
                return RC_OK;
            }
            
            if (argObject.getOtherArgs().size() == 0) {
                error("Missing Spring config argument");
                return RC_INIT;
            }
            
            initLogging(argObject);

            argObject.validate();

        } catch (final Exception e) {
            error(e.getMessage());
            return RC_INIT;
        }

        return doRun(argObject);
    }
    
    /**
     * Initialize the logging subsystem.
     * 
     * @param args command line arguments
     */
    protected void initLogging(@Nonnull final T args) {
        if (args.getLoggingConfiguration() != null) {
            System.setProperty("logback.configurationFile", args.getLoggingConfiguration());
        } else if (args.isVerboseOutput()) {
            System.setProperty("logback.configurationFile", "logger-verbose.xml");
        } else if (args.isQuietOutput()) {
            System.setProperty("logback.configurationFile", "logger-quiet.xml");
        } else {
            System.setProperty("logback.configurationFile", "logger-normal.xml");
        }
    }

    /**
     * Merge in properties from the resource.
     * 
     * @param sink if non-null use this instance as the target
     * @param resource the resource
     * @return properties loaded from the resource or {@code  null} if loading failed
     */
    @Nullable public Properties loadProperties(@Nullable final Properties sink, @Nonnull final Resource resource) {
        Constraint.isNotNull(resource, "Resource cannot be null");
        try {
            final Properties properties;
            if (sink != null) {
                properties = sink;
            } else {
                properties = new Properties();
            }
            PropertiesLoaderUtils.fillProperties(properties, resource);
            return properties;
        } catch (final IOException e) {
            getLogger().warn("Unable to load properties from resource '{}'", resource, e);
            return null;
        }
    }

    /**
     * The execution method to override.
     * 
     * The default implementation handles Spring context creation.
     * 
     * @param args input arguments
     * 
     * @return exit code
     */
    protected int doRun(@Nonnull final T args) {
        try {
            final ResourceLoader loader = new PreferFileSystemResourceLoader();
            final Resource config = loader.getResource(args.getOtherArgs().get(0));
            
            getLogger().debug("Initializing Spring context with configuration file {}", config.getURI());

            final List<Resource> resources =
                    args.getPropertyFiles().stream().map(loader::getResource).collect(Collectors.toUnmodifiableList());
            final List<PropertySource<?>> propertySources = new ArrayList<>(resources.size());
            resources.forEach(r -> {
                try {
                    propertySources.add(new ResourcePropertySource(r));
                } catch (final IOException e) {
                    if (args.isVerboseOutput()) {
                        getLogger().error("Unable to load properties from {}", r, e);
                    } else {
                        getLogger().error("Unable to load properties from {}", r, e.getMessage());
                    }
                }
            });
            
            applicationContext = new ApplicationContextBuilder()
                    .setServiceConfiguration(config)
                    .setPropertySources(propertySources)
                    .build();
            
            // Register a shutdown hook for the context, so that beans will be
            // correctly destroyed before the CLI exits.
            applicationContext.registerShutdownHook();
        } catch (final Exception e) {
            if (args.isVerboseOutput()) {
                getLogger().error("Unable to initialize Spring context", e);
            } else {
                getLogger().error("Unable to initialize Spring context", e.getMessage());
            }
            return RC_INIT;
        }
        
        return RC_OK;
    }
    
    /**
     * Get the class of the argument object to instantiate.
     * 
     * @return argument class
     */
    @Nonnull protected abstract Class<T> getArgumentClass();
    
    /**
     * Return an appropriate version value.
     * 
     * @return a version string
     */
    @Nonnull @NotEmpty protected abstract String getVersion();
       
    /**
     * Get logger.
     * 
     * @return logger
     */
    @Nonnull protected abstract Logger getLogger();
    
    /**
     * Prints the error message to STDERR.
     * 
     * @param error the error message
     */
    private static void error(@Nonnull @NotEmpty final String error) {
        System.err.println(error);
        System.err.flush();
        System.out.println();
        System.out.flush();
    }
    
}