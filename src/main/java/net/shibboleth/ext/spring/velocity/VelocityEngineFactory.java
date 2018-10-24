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

package net.shibboleth.ext.spring.velocity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import net.shibboleth.utilities.java.support.velocity.SLF4JLogChute;

/**
 * Factory that configures a VelocityEngine. Can be used standalone,
 * but typically you will either use {@link VelocityEngineFactoryBean}
 * for preparing a VelocityEngine as bean reference, or
 * {@link org.springframework.web.servlet.view.velocity.VelocityConfigurer}
 * for web views.
 *
 * <p>The optional "configLocation" property sets the location of the Velocity
 * properties file, within the current application. Velocity properties can be
 * overridden via "velocityProperties", or even completely specified locally,
 * avoiding the need for an external properties file.
 *
 * <p>The "resourceLoaderPath" property can be used to specify the Velocity
 * resource loader path via Spring's Resource abstraction, possibly relative
 * to the Spring application context.
 *
 * <p>If "overrideLogging" is true (the default), the VelocityEngine will be
 * configured to log via Commons Logging, that is, using
 * {@link CommonsLogLogChute} as log system.
 *
 * <p>The simplest way to use this class is to specify a
 * {@link #setResourceLoaderPath(String) "resourceLoaderPath"}; the
 * VelocityEngine typically then does not need any further configuration.
 *
 * @author Juergen Hoeller
 * @see #setConfigLocation
 * @see #setVelocityProperties
 * @see #setResourceLoaderPath
 * @see #setOverrideLogging
 * @see #createVelocityEngine
 * @see VelocityEngineFactoryBean
 * @see org.springframework.web.servlet.view.velocity.VelocityConfigurer
 * @see org.apache.velocity.app.VelocityEngine
 * 
 * @since 6.0.0
 */
public class VelocityEngineFactory {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(VelocityEngineFactory.class);

    /** Source of configuration. */
    @Nullable private Resource configLocation;

    /** Configuration properties. */
    @Nonnull private final Map<String, Object> velocityProperties;

    /** Path to load resources from. */
    @Nullable private String resourceLoaderPath;

    /** Resource loader. */
    @Nullable private ResourceLoader resourceLoader;

    /** Whether to favor file system lookup first. */
    private boolean preferFileSystemAccess;

    /** Override logging? */
    private boolean overrideLogging;
    
    /** Constructor. */
    public VelocityEngineFactory() {
        velocityProperties = new HashMap<String, Object>();
        resourceLoader = new DefaultResourceLoader();
        preferFileSystemAccess = true;
        overrideLogging = true;
    }

    /**
     * Set the location of the Velocity config file.
     * 
     * <p>Alternatively, you can specify all properties locally.</p>
     * 
     * @param location configuration resource
     * 
     * @see #setVelocityProperties
     * @see #setResourceLoaderPath
     */
    public void setConfigLocation(@Nullable final Resource location) {
        configLocation = location;
    }

    /**
     * Set Velocity properties, like "file.resource.loader.path".
     * 
     * <p>Can be used to override values in a Velocity config file,
     * or to specify all necessary properties locally.</p>
     * 
     * <p>Note that the Velocity resource loader path also be set to any
     * Spring resource location via the "resourceLoaderPath" property.
     * Setting it here is just necessary when using a non-file-based
     * resource loader.</p>
     * 
     * @param props additional properties
     * 
     * @see #setVelocityPropertiesMap
     * @see #setConfigLocation
     * @see #setResourceLoaderPath
     */
    public void setVelocityProperties(@Nullable final Properties props) {
        CollectionUtils.mergePropertiesIntoMap(props, velocityProperties);
    }

    /**
     * Set Velocity properties as Map, to allow for non-String values
     * like "ds.resource.loader.instance".
     * 
     * @param map properties as map
     * 
     * @see #setVelocityProperties
     */
    public void setVelocityPropertiesMap(@Nullable final Map<String,Object> map) {
        if (map != null) {
            velocityProperties.putAll(map);
        }
    }

    /**
     * Set the Velocity resource loader path via a Spring resource location.
     * 
     * <p>Accepts multiple locations in Velocity's comma-separated path style.</p>
     * 
     * <p>When populated via a String, standard URLs like "file:" and "classpath:"
     * pseudo URLs are supported, as understood by ResourceLoader. Allows for
     * relative paths when running in an ApplicationContext.</p>
     * 
     * <p>Will define a path for the default Velocity resource loader with the name
     * "file". If the specified resource cannot be resolved to a {@code java.io.File},
     * a generic SpringResourceLoader will be used under the name "spring", without
     * modification detection.</p>
     * 
     * <p>Note that resource caching will be enabled in any case. With the file
     * resource loader, the last-modified timestamp will be checked on access to
     * detect changes. With SpringResourceLoader, the resource will be cached
     * forever (for example for class path resources).</p>
     * 
     * <p>To specify a modification check interval for files, use Velocity's
     * standard "file.resource.loader.modificationCheckInterval" property. By default,
     * the file timestamp is checked on every access (which is surprisingly fast).
     * Of course, this just applies when loading resources from the file system.</p>
     * 
     * <p>To enforce the use of SpringResourceLoader, i.e. to not resolve a path
     * as file system resource in any case, turn off the "preferFileSystemAccess"
     * flag. See the latter's javadoc for details.</p>
     * 
     * @param paths comma-separated paths
     * 
     * @see #setResourceLoader
     * @see #setVelocityProperties
     * @see #setPreferFileSystemAccess
     * @see SpringResourceLoader
     * @see org.apache.velocity.runtime.resource.loader.FileResourceLoader
     */
    public void setResourceLoaderPath(@Nullable final String paths) {
        resourceLoaderPath = paths;
    }

    /**
     * Set the Spring ResourceLoader to use for loading Velocity template files.
     * 
     * <p>The default is DefaultResourceLoader. Will get overridden by the
     * ApplicationContext if running in a context.</p>
     * 
     * @param loader resource loader
     * 
     * @see org.springframework.core.io.DefaultResourceLoader
     * @see org.springframework.context.ApplicationContext
     */
    public void setResourceLoader(@Nullable final ResourceLoader loader) {
        resourceLoader = loader;
    }

    /**
     * Set whether to prefer file system access for template loading.
     * 
     * <p>File system access enables hot detection of template changes.</p>
     * 
     * <p>If this is enabled, VelocityEngineFactory will try to resolve the
     * specified "resourceLoaderPath" as file system resource (which will work
     * for expanded class path resources and ServletContext resources too).</p>
     * 
     * <p>Default is "true". Turn this off to always load via SpringResourceLoader
     * (i.e. as stream, without hot detection of template changes), which might
     * be necessary if some of your templates reside in an expanded classes
     * directory while others reside in jar files.</p>
     * 
     * @param flag flag to set
     * 
     * @see #setResourceLoaderPath
     */
    public void setPreferFileSystemAccess(final boolean flag) {
        preferFileSystemAccess = flag;
    }

    /**
     * Set whether Velocity should log via SLF4J, i.e. whether Velocity's
     * log system should be set to {@link SLF4JLogChute}.
     * 
     * <p>Default is "true".</p>
     * 
     * @param flag flag to set
     */
    public void setOverrideLogging(final boolean flag) {
        overrideLogging = flag;
    }

    /**
     * Prepare the VelocityEngine instance and return it.
     * 
     * @return the VelocityEngine instance
     * 
     * @throws IOException if the config file wasn't found
     * @throws VelocityException on Velocity initialization failure
     */
    @Nonnull public VelocityEngine createVelocityEngine() throws IOException, VelocityException {
        
        final Map<String,Object> props = new HashMap<>();

        // Load config file if set.
        if (configLocation != null) {
            log.info("Loading Velocity config from '{}'", configLocation);
            CollectionUtils.mergePropertiesIntoMap(PropertiesLoaderUtils.loadProperties(configLocation), props);
        }

        // Merge local properties if set.
        if (!velocityProperties.isEmpty()) {
            props.putAll(velocityProperties);
        }

        final VelocityEngine velocityEngine = newVelocityEngine();

        // Set a resource loader path, if required.
        if (resourceLoaderPath != null) {
            initVelocityResourceLoader(velocityEngine, resourceLoaderPath);
        }
        
        // Log via SLF4J?
        if (overrideLogging) {
            velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new SLF4JLogChute());
        }

        // Apply properties to VelocityEngine.
        for (final Map.Entry<String,Object> entry : props.entrySet()) {
            velocityEngine.setProperty(entry.getKey(), entry.getValue());
        }

        postProcessVelocityEngine(velocityEngine);

        // Perform actual initialization.
        velocityEngine.init();

        return velocityEngine;
    }

    /**
     * Return a new VelocityEngine.
     * 
     * <p>Subclasses can override this for
     * custom initialization, or for using a mock object for testing.</p>
     * 
     * <p>Called by {@code createVelocityEngine()}.</p>
     * 
     * @return the VelocityEngine instance
     * 
     * @throws IOException if a config file wasn't found
     * @throws VelocityException on Velocity initialization failure
     * 
     * @see #createVelocityEngine()
     */
    @Nonnull protected VelocityEngine newVelocityEngine() throws IOException, VelocityException {
        return new VelocityEngine();
    }

    /**
     * Initialize a Velocity resource loader for the given VelocityEngine:
     * either a standard Velocity FileResourceLoader or a SpringResourceLoader.
     * 
     * @param velocityEngine the VelocityEngine to configure
     * @param loaderPath the path to load Velocity resources from
     * 
     * @see org.apache.velocity.runtime.resource.loader.FileResourceLoader
     * @see SpringResourceLoader
     * @see #initSpringResourceLoader
     * @see #createVelocityEngine()
     */
    protected void initVelocityResourceLoader(@Nonnull final VelocityEngine velocityEngine,
            @Nullable final String loaderPath) {
        if (preferFileSystemAccess && resourceLoader != null) {
            // Try to load via the file system, fall back to SpringResourceLoader
            // (for hot detection of template changes, if possible).
            try {
                final StringBuilder resolvedPath = new StringBuilder();
                final String[] paths = StringUtils.commaDelimitedListToStringArray(loaderPath);
                for (int i = 0; i < paths.length; i++) {
                    final String path = paths[i];
                    final Resource resource = resourceLoader.getResource(path);
                    
                 // Will fail if not resolvable in the file system.
                    final File file = resource.getFile();  
                    
                    if (log.isDebugEnabled()) {
                        log.debug("Resource loader path '{}' resolved to file '{}'", path, file.getAbsolutePath());
                    }
                    
                    resolvedPath.append(file.getAbsolutePath());
                    if (i < paths.length - 1) {
                        resolvedPath.append(',');
                    }
                }
                velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
                velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
                velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, resolvedPath.toString());
            } catch (final IOException ex) {
                log.debug("Cannot resolve resource loader path '{}' to [java.io.File], will use SpringResourceLoader",
                        loaderPath, ex);
                initSpringResourceLoader(velocityEngine, loaderPath);
            }
        } else {
            // Always load via SpringResourceLoader (without hot detection of template changes).
            log.debug("Filesystem access not preferred, will use SpringResourceLoader");
            initSpringResourceLoader(velocityEngine, loaderPath);
        }
    }

    /**
     * Initialize a SpringResourceLoader for the given VelocityEngine.
     * 
     * <p>Called by {@code initVelocityResourceLoader}.</p>
     * 
     * @param velocityEngine the VelocityEngine to configure
     * @param path the path to load Velocity resources from
     * 
     * @see SpringResourceLoader
     * @see #initVelocityResourceLoader
     */
    protected void initSpringResourceLoader(@Nonnull final VelocityEngine velocityEngine,
            @Nullable final String path) {
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, SpringResourceLoader.NAME);
        velocityEngine.setProperty(SpringResourceLoader.SPRING_RESOURCE_LOADER_CLASS,
                SpringResourceLoader.class.getName());
        velocityEngine.setProperty(SpringResourceLoader.SPRING_RESOURCE_LOADER_CACHE, "true");
        velocityEngine.setApplicationAttribute(SpringResourceLoader.SPRING_RESOURCE_LOADER, resourceLoader);
        velocityEngine.setApplicationAttribute(SpringResourceLoader.SPRING_RESOURCE_LOADER_PATH, path);
    }

    /**
     * To be implemented by subclasses that want to perform custom
     * post-processing of the VelocityEngine after the FactoryBean
     * performed its default configuration (but before VelocityEngine.init).
     * 
     * @param velocityEngine the current VelocityEngine
     * 
     * @throws IOException if a config file wasn't found
     * @throws VelocityException on Velocity initialization failure
     * 
     * @see #createVelocityEngine()
     * @see org.apache.velocity.app.VelocityEngine#init
     */
    protected void postProcessVelocityEngine(@Nonnull final VelocityEngine velocityEngine)
            throws IOException, VelocityException {
    }

}