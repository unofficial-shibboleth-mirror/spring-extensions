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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;

import com.beust.jcommander.JCommander;

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
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

    /** ANSI color codes. */
    public enum ANSIColors {
        
        /** ANSI reset. */
        ANSI_RESET("\u001B[0m"),

        /** ANSI black. */
        ANSI_BLACK("\u001B[30m"),
        
        /** ANSI red. */
        ANSI_RED("\u001B[31m"),
        
        /** ANSI red. */
        ANSI_GREEN("\u001B[32m"),
        
        /** ANSI red. */
        ANSI_YELLOW("\u001B[33m"),
        
        /** ANSI red. */
        ANSI_BLUE("\u001B[34m"),
        
        /** ANSI red. */
        ANSI_PURPLE("\u001B[35m"),
        
        /** ANSI red. */
        ANSI_CYAN("\u001B[36m"),
        
        /** ANSI red. */
        ANSI_WHITE("\u001B[37m");
        
        /** Code string. */
        @Nonnull @NotEmpty private final String codestring;
        
        /**
         * Constructor.
         *
         * @param s code string
         */
        ANSIColors(@Nonnull @NotEmpty final String s) {
            codestring = s;
        }
        
        /** {@inheritDoc} */
        public String toString() {
            return codestring;
        }
    };
    
    /** Spring context. */
    @Nullable private GenericApplicationContext applicationContext;

    /** Optional Context initialized. */
    @Nullable private ApplicationContextInitializer<? super FilesystemGenericApplicationContext> contextInitializer;
    
    /**
     * Set a context initializer.
     * 
     * @param initializer what to set
     */
    protected void setContextInitializer(
            @Nonnull final ApplicationContextInitializer<? super FilesystemGenericApplicationContext> initializer) {
        contextInitializer = Constraint.isNotNull(initializer, "Injected ContextInitializer cannot be null");
    }

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
     * Return any additional resources that should be prepended to that supplied by the caller.
     * 
     * @return the resources
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive protected List<Resource> getAdditionalSpringResources() {
        return Collections.emptyList();
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
            
            initLogging(argObject);

            argObject.validate();

        } catch (final Exception e) {
            error(e.getMessage());
            return RC_INIT;
        }

        return doRun(argObject);
    }
    
    /**
     * Set the logback configuration to a specific location.
     * 
     * <p>
     * Note that this <strong>must</strong> be done before the
     * first logger is retrieved.
     * </p>
     *
     * @param value logback configuration location to set
     */
    private void setLoggingProperty(@Nonnull final String value) {
        System.setProperty("logback.configurationFile", value);
    }
    
    /**
     * Set the logback configuration to a specific package-local resource.
     *
     * @param value name of resource to use as the logback configuration file
     */
    private void setLoggingToLocalResource(@Nonnull final String value) {
        setLoggingProperty("net/shibboleth/ext/spring/cli/" + value);
    }

    /**
     * Initialize the logging subsystem.
     * 
     * @param args command line arguments
     */
    protected void initLogging(@Nonnull final T args) {
        if (args.getLoggingConfiguration() != null) {
            setLoggingProperty(args.getLoggingConfiguration());
        } else if (args.isVerboseOutput()) {
            setLoggingToLocalResource("logger-verbose.xml");
        } else if (args.isQuietOutput()) {
            setLoggingToLocalResource("logger-quiet.xml");
        } else {
            setLoggingToLocalResource("logger-normal.xml");
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
            final List<Resource> additionalConfigs = getAdditionalSpringResources();

            final List<Resource> configs = new ArrayList<>(1+additionalConfigs.size());
            if (args.getOtherArgs().size() > 0) {
                configs.add(loader.getResource(args.getOtherArgs().get(0)));
            }
            configs.addAll(additionalConfigs);

            getLogger().debug("Initializing Spring context with {}", configs);

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
            
            final ApplicationContextBuilder builder = new ApplicationContextBuilder()
                    .setServiceConfigurations(configs)
                    .setPropertySources(propertySources);

            if (contextInitializer != null) {
                builder.setContextInitializer(contextInitializer);
            }
            applicationContext = builder.build();

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