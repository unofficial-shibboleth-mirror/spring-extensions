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

import javax.annotation.Nonnull;

/**
 * An extension of {@link FileSystemXmlWebApplicationContext} that defers property placeholder resolution of config
 * locations until after property sources have been initialized. This application context is intended to be used with an
 * application context initializer which adds a properties file property source to the application context environment.
 */
public class DeferPlaceholderFileSystemXmlWebApplicationContext extends FileSystemXmlWebApplicationContext {

    /** Whether property sources have been initialized. */
    @Nonnull private boolean propertySourcesInitialized;

    /** Constructor. */
    public DeferPlaceholderFileSystemXmlWebApplicationContext() {
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Resolve config locations property placeholders after property sources have been initialized.
     * </p>
     */
    @Override protected void initPropertySources() {
        super.initPropertySources();
        propertySourcesInitialized = true;
        setConfigLocations(getConfigLocations());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Resolve property placeholders only if property sources have been initialized, otherwise return the path
     * unchanged.
     * </p>
     */
    @Override protected String resolvePath(String path) {
        if (propertySourcesInitialized) {
            return super.resolvePath(path);
        }
        return path;
    }

}
