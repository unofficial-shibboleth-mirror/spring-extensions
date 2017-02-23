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
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Test bean to canary out behavior of reloadable bean service.
 */
public class NonReloadableTestBean extends AbstractInitializableComponent {

    @Nonnull private final Logger log = LoggerFactory.getLogger(NonReloadableTestBean.class);
    
    @Nonnull @NotEmpty private final String id;
    
    @Nullable private ReloadableTestBean child;
    
    public NonReloadableTestBean(@Nonnull @NotEmpty final String name) {
        id = name;
        log.debug("NonReloadableTestBean {} created", id);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        log.debug("NonReloadableTestBean {} initialized", id);
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        log.debug("NonReloadableTestBean {} destroyed", id);
        
        super.doDestroy();
    }
    
    public ReloadableTestBean getChild() {
        log.debug("NonReloadableTestBean {} child getter called", id);
        
        return child;
    }
    
    public void setChild(ReloadableTestBean b) {
        child = b;
    }
    
    public int getValue() {
        return getChild().getValue();
    }
    
}