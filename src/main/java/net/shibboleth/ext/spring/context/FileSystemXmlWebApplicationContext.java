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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * An extension of {@link XmlWebApplicationContext} that is biased in favor of the filesystem such that bare resource
 * paths are assumed to be files rather than classpath resources.
 */
public class FileSystemXmlWebApplicationContext extends XmlWebApplicationContext {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FileSystemXmlWebApplicationContext.class);

    /** Constructor. */
    public FileSystemXmlWebApplicationContext() {
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Overrides the standard behavior of path-only resources and treats them as file paths if the path exists. Note
     * that this differs from the ordinary Spring contexts that default to file paths because paths are treated as
     * absolute if they are in fact absolute.
     * </p>
     */
    @Override protected Resource getResourceByPath(String path) {
        final Resource r = new FileSystemResource(path);
        if (r.exists()) {
            return r;
        }
        return super.getResourceByPath(path);
    }

}
