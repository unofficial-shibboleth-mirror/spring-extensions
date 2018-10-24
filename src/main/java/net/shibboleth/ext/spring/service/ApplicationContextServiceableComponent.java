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

import javax.annotation.Nonnull;

import org.springframework.context.ApplicationContext;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Wraps a Spring {@link ApplicationContext} so it can itself be exposed as a serviceable component.
 * 
 * @since 5.4.0
 */
public class ApplicationContextServiceableComponent extends AbstractServiceableComponent<ApplicationContext> {
    
    /** {@inheritDoc} */
    public void setId(@Nonnull @NotEmpty final String componentId) {
        super.setId(componentId);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public ApplicationContext getComponent() {
        final ApplicationContext context = getApplicationContext();
        return Constraint.isNotNull(context, "ApplicationContext not yet set");
    }

}