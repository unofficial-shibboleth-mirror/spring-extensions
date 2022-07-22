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

package net.shibboleth.ext.spring.config;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.ApplicationContext;

/**
 * Custom Spring bean {@link Scope} that directs bean requests into a managed {@link ApplicationContext}.
 * 
 * @since 5.4.0
 */
public class ReloadableScope implements Scope {

    /** Scope indicating reloadability. */
    @Nonnull @NotEmpty public static final String SCOPE_RELOADABLE = "reloadable";
    
    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReloadableScope.class);
    
    /** Managed context service wrapper. */
    @Nonnull private final ReloadableService<ApplicationContext> reloadableService;
    
    /**
     * Constructor.
     *
     * @param service instance of Spring context to wrap
     */
    public ReloadableScope(
            @Nonnull @ParameterName(name="service") final ReloadableService<ApplicationContext> service) {
        reloadableService = Constraint.isNotNull(service, "ReloadableService cannot be null");
    }

    /** {@inheritDoc} */
    public Object get(final String name, final ObjectFactory<?> objectFactory) {
        log.debug("Accessing reloadable bean instance '{}'", name);
        try (final ServiceableComponent<ApplicationContext> component = reloadableService.getServiceableComponent()) {
            return component.getComponent().getBean(name);
        }
    }

    /** {@inheritDoc} */
    public Object remove(final String name) {
        throw new UnsupportedOperationException("No support for object removal");
    }

    /** {@inheritDoc} */
    public void registerDestructionCallback(final String name, final Runnable callback) {
        log.warn("Ignoring unsupported destruction callback for '{}'", name);
    }

    /** {@inheritDoc} */
    public Object resolveContextualObject(final String key) {
        return null;
    }

    /** {@inheritDoc} */
    public String getConversationId() {
        return null;
    }

}