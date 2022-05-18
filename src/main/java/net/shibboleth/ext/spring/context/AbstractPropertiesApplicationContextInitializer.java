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

package net.shibboleth.ext.spring.context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * An {@link ApplicationContextInitializer} which appends properties to the application context's environment.
 * 
 * <p>Properties are loaded from {@link  #getSearchTarget()} as well as additional property files specified by
 * {@link #getAdditionalPropertiesPropertyName()} and/or by locating all files under a designated location that
 * end in ".properties".</p>
 * 
 * <p>The {@link #getSearchTarget()} file is searched for in a well-known location returned by
 * {@link #getSearchLocation()}.</p>
 * 
 * <p>If not already set, {@link #getHomePropertyName()} will be set to the first search location in which the
 * {@link #getSearchTarget()} file is found.</p>
 * 
 * <p>A {@link ConstraintViolationException} will be thrown if the property files can not be found or loaded and
 * {@link #isFailFast(ConfigurableApplicationContext)} returns true.</p>
 * 
 * @since 7.0.0
 */
public abstract class AbstractPropertiesApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** Class logger. */
    @Nonnull private static final Logger LOG =
            LoggerFactory.getLogger(AbstractPropertiesApplicationContextInitializer.class);

    /**
     * Get the name of the property used to identify the "home" of the application's configuration
     * tree (e.g., "idp.home" or "sp.home").
     * 
     * @return home property name
     */
    @Nonnull @NotEmpty protected abstract String getHomePropertyName();
    
    /**
     * Get the target resource to be searched for (e.g., "/conf/idp.properties").
     * 
     * @return the target resource to be searched for
     */
    @Nonnull @NotEmpty protected abstract String getSearchTarget();

    /**
     * Get the well known search location (e.g., "/opt/shibboleth-idp").
     * 
     * @return the well known search locations
     */
    @Nonnull @NotEmpty protected abstract String getSearchLocation();
    
    /**
     * Get the name of the property used to control fail-fast behavior (e.g., "idp.initializer.failFast").
     * 
     * @return failfast property name
     */
    @Nonnull @NotEmpty protected abstract String getFailFastPropertyName();
    
    /**
     * Get the name of the property identifying the additional property resources to load
     * (e.g., "idp.additionalProperties").
     * 
     * @return name of additional properties property
     */
    @Nonnull @NotEmpty protected abstract String getAdditionalPropertiesPropertyName();

    /**
     * Get the name of the property determining whether to enable auto-search
     * (e.g., "idp.searchForProperties").
     * 
     * @return name of additional properties property
     */
    @Nonnull @NotEmpty protected abstract String getAutoSearchPropertyName();

    /** {@inheritDoc} */
    public void initialize(@Nonnull final ConfigurableApplicationContext applicationContext) {
        LOG.debug("Initializing application context '{}'", applicationContext);

        // TODO: Override default property replacement syntax.
        // We can't do this now because it would break web.xml's use of ${idp.home}
        // If we end up breaking web.xml later, I think we could force that in line.
        // See IDP-1642
        // applicationContext.getEnvironment().setPlaceholderPrefix("%{");
        // applicationContext.getEnvironment().setPlaceholderSuffix("}");
        
        final String searchLocation = selectSearchLocation(applicationContext);
        LOG.debug("Attempting to find '{}' at search location '{}'", getSearchTarget(), searchLocation);

        final String searchPath = searchLocation + getSearchTarget();

        LOG.debug("Attempting to find resource '{}'", searchPath);
        final Resource resource = applicationContext.getResource(searchPath);

        if (resource.exists()) {
            LOG.debug("Found resource '{}' at search path '{}'", resource, searchPath);

            final Properties properties = loadProperties(null, resource);
            if (properties == null) {
                if (isFailFast(applicationContext)) {
                    LOG.error("Unable to load properties from resource '{}'", resource);
                    throw new ConstraintViolationException("Unable to load properties from resource");
                }
                LOG.warn("Unable to load properties from resource '{}'", resource);
                return;
            }

            if ("classpath:".equals(searchLocation) || resource instanceof ClassPathResource) {
                setHomeProperty(searchLocation, properties);
            } else {
                String searchLocationAbsolutePath = Paths.get(searchLocation).toAbsolutePath().toString();
                // Minimal normalization required on Windows to allow SWF's flow machinery to work.
                // Just replace backslashes with forward slashes.
                if (File.separatorChar == '\\') {
                    searchLocationAbsolutePath = searchLocationAbsolutePath.replace('\\', '/');
                }
                setHomeProperty(searchLocationAbsolutePath, properties);
            }

            loadAdditionalPropertySources(applicationContext, searchLocation, properties);

            logProperties(properties);

            appendPropertySource(applicationContext, resource.toString(), properties);

        } else if (isFailFast(applicationContext)) {
            LOG.error("Unable to find '{}' at '{}'", getSearchTarget(), searchLocation);
            throw new ConstraintViolationException(
                    "Unable to find '" + getSearchTarget() + "' at '" + searchLocation + "'");
        } else {
            LOG.warn("Unable to find '{}' at '{}'", getSearchTarget(), searchLocation);
        }
    }

    /**
     * Select the location used to search for the target. Prefers the user-defined search location defined by
     * {@link #getHomePropertyName()} in the application context. Defaults to the well-known search location
     * returned from {@link #getSearchLocation()}.
     * 
     * @param applicationContext the application context
     * @return the search location used to search for the target
     * @throws ConstraintViolationException if the user-defined search location is empty or ends with '/' and
     *             {@link #isFailFast(ConfigurableApplicationContext)} is true
     */
    @Nonnull @NotEmpty protected String selectSearchLocation(
            @Nonnull final ConfigurableApplicationContext applicationContext) {

        Constraint.isNotNull(applicationContext, "Application context cannot be null");
        final String homeProperty = applicationContext.getEnvironment().getProperty(getHomePropertyName());
        if (homeProperty != null && isFailFast(applicationContext)) {
            Constraint.isNotEmpty(homeProperty, "idp.home cannot be empty");
            Constraint.isFalse(homeProperty.endsWith("/"), "idp.home cannot end with '/'");
        }
        return (homeProperty != null) ? homeProperty : getSearchLocation();
    }

    /**
     * Load properties from the resource.
     * 
     * @param sink if non-null use this instance as the target
     * @param resource the resource
     * @return properties loaded from the resource or {@code  null} if loading failed
     */
    @Nullable protected Properties loadProperties(@Nullable final Properties sink, @Nonnull final Resource resource) {
        Constraint.isNotNull(resource, "Resource cannot be null");
        try {
            final Properties holder = new Properties();
            try (final InputStream is = resource.getInputStream()) {
                final String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".xml")) {
                    holder.loadFromXML(is);
                } else {
                    holder.load(is);
                }
            }
            
            if (sink == null) {
                return holder;
            }

            // Check for duplicates before adding.
            for (final Map.Entry<Object,Object> entry : holder.entrySet()) {
                if (sink.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
                    LOG.warn("Ignoring duplicate property '{}'", entry.getKey());
                }
            }

            return sink;
        } catch (final IOException e) {
            LOG.warn("Unable to load properties from resource '{}'", resource, e);
            return null;
        }
    }

   /**
     * Find out all the additional property files we need to load.
     *   
     * @param searchLocation Where to search from
     * @param properties the content of idp.properties so far
     * 
     * @return a collection of paths
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable protected Collection<String> getAdditionalSources(
            @Nonnull final String searchLocation, @Nonnull final Properties properties) {
       final Collection<String> sources = new ArrayList<>();
       
       final Boolean autosearch = Boolean.valueOf(properties.getProperty(getAutoSearchPropertyName(), "false"));
       if (autosearch) {
           final Path searchRoot = Path.of(searchLocation).resolve("conf");
           if (searchRoot.toFile().isDirectory()) {
               final Path registryRoot = searchRoot.resolve("attributes");
               final String idpPropertiesNative = Path.of(getSearchTarget()).toString();
               try (final Stream<Path> paths = Files.find(searchRoot, Integer.MAX_VALUE,
                       new BiPredicate<Path,BasicFileAttributes>() {
                               public boolean test(final Path path, final BasicFileAttributes u) {
                                   final String pathAsString = path.toString();
                                   // convert back and forth to handle different dir separators
                                   if (u.isRegularFile()
                                           && path.getFileName().toString().endsWith(".properties")
                                           && !pathAsString.endsWith(idpPropertiesNative)
                                           && !pathAsString.startsWith(registryRoot.toString())) {
                                       LOG.info("Including auto-located properties in {}", path);
                                       return true;
                                   }
                                   return false;
                               }
                       }, FileVisitOption.FOLLOW_LINKS)) {
                   
                   sources.addAll(paths.map(Path::toString).collect(Collectors.toUnmodifiableList()));
               } catch (final IOException e) {
                   LOG.error("Error searching for additional properties", e);
               }
           }
       }
       
       final String additionalSources = properties.getProperty(getAdditionalPropertiesPropertyName());
       if (additionalSources != null) {
           final String[] split = additionalSources.split(",");
           for (final String s : split) {
               final String trimmedSource = StringSupport.trimOrNull(s);
               if (trimmedSource != null) {
                   sources.add(searchLocation + trimmedSource);
               }
           }
       }
       return sources;
   }
    
    /**
     * Load additional property sources.
     * 
     * <p>File names of additional property sources are defined by {@link #getAdditionalPropertiesPropertyName()},
     * and are resolved relative to the given search location.</p>
     * 
     * @param applicationContext the application context
     * @param searchLocation the location from which additional property sources are resolved
     * @param properties the properties to be filled with additional property sources
     * @throws ConstraintViolationException if an error occurs loading the additional property sources and
     *             {@link #isFailFast(ConfigurableApplicationContext)} is true
     */
    protected void loadAdditionalPropertySources(@Nonnull final ConfigurableApplicationContext applicationContext,
            @Nonnull final String searchLocation, @Nonnull final Properties properties) {
        
        for (final String source : getAdditionalSources(searchLocation, properties)) {
            LOG.debug("Attempting to load properties from resource '{}'", source);
            final Resource additionalResource = applicationContext.getResource(source);
            if (additionalResource.exists()) {
                LOG.debug("Found property resource '{}'", additionalResource);
                if (loadProperties(properties, additionalResource) == null) {
                    if (isFailFast(applicationContext)) {
                        LOG.error("Unable to load properties from resource '{}'", additionalResource);
                        throw new ConstraintViolationException("Unable to load properties from resource");
                    }
                    LOG.warn("Unable to load properties from resource '{}'", additionalResource);
                    continue;
                }
            } else {
                LOG.warn("Unable to find property resource '{}' (check {}?)", additionalResource,
                        getAdditionalPropertiesPropertyName());
            }
        }
    }
    
    /**
     * Log property names and values at debug level, suppressing properties whose name matches 'password',
     * 'credential', 'secret', or 'salt'.
     * 
     * @param properties the properties to log
     */
    protected void logProperties(@Nonnull final Properties properties) {
        if (LOG.isDebugEnabled()) {
            final Pattern pattern = Pattern.compile("password|credential|secret|salt|key", Pattern.CASE_INSENSITIVE);
            for (final String name : new TreeSet<>(properties.stringPropertyNames())) {
                final Object value = pattern.matcher(name).find() ? "<suppressed>" : properties.get(name);
                LOG.debug("Loaded property '{}'='{}'", name, value);
            }
        }
    }

    /**
     * Add property source to the application context environment with lowest precedence.
     * 
     * @param applicationContext the application context
     * @param name the name of the property source to be added to the application context
     * @param properties the properties added to the application context
     */
    protected void appendPropertySource(@Nonnull final ConfigurableApplicationContext applicationContext,
            @Nonnull final String name, @Nonnull final Properties properties) {
        applicationContext.getEnvironment().getPropertySources()
                .addLast(new PropertiesPropertySource(name, properties));
    }

    /**
     * Set the {@link #getHomePropertyName()} property to the given path if not already set.
     * 
     * @param path the property value
     * @param properties the properties
     */
    protected void setHomeProperty(@Nonnull final String path, @Nonnull final Properties properties) {
        Constraint.isNotNull(path, "Path cannot be null");
        Constraint.isNotNull(properties, "Properties cannot be null");

        if (properties.getProperty(getHomePropertyName()) != null) {
            LOG.debug("Will not set '{}' property because it is already set.", getHomePropertyName());
            return;
        }

        LOG.debug("Setting '{}' property to '{}'", getHomePropertyName(), path);

        properties.setProperty(getHomePropertyName(), path);
    }

    /**
     * Whether we fail immediately if the config is bogus. Defaults to true. Controlled by the value of the
     * {@link #getFailFastPropertyName()}.
     * <b>This functionality is reserved for use in tests </b> where is is usually used to allow
     * tests to be run in the presence of partial configuration.
     * 
     * @param applicationContext the application context
     * @return whether we fail immediately if the config is faulty or incomplete.
     */
    protected boolean isFailFast(@Nonnull final ConfigurableApplicationContext applicationContext) {
        Constraint.isNotNull(applicationContext, "Application context cannot be null");
        final String failFast = applicationContext.getEnvironment().getProperty(getFailFastPropertyName());
        return (failFast == null) ? true : Boolean.parseBoolean(failFast);
    }

}