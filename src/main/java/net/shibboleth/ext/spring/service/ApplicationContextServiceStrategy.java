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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Strategy for summoning up an {@link ApplicationContextServiceableComponent} wrapper
 * around a populated {@link ApplicationContext}.
 * 
 * @since 5.4.0
 */
public class ApplicationContextServiceStrategy implements
        Function<ApplicationContext, ServiceableComponent<ApplicationContext>> {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ApplicationContextServiceStrategy.class);
    
    /** {@inheritDoc} */
    @Nullable public ServiceableComponent<ApplicationContext> apply(@Nullable final ApplicationContext appContext) {

        if (appContext != null) {
            final ApplicationContextServiceableComponent wrapper = new ApplicationContextServiceableComponent();
            wrapper.setApplicationContext(appContext);
            wrapper.setId(appContext.getId());
            try {
                wrapper.initialize();
                return wrapper;
            } catch (final ComponentInitializationException e) {
                log.error("Unable to initialize component wrapper for ApplicationContext", e);
            }
        }
        return null;
    }
    
}