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

package net.shibboleth.ext.spring.factory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Factory bean that proxies the creation of a bean through a managed/reloadable
 * {@link ApplicationContext}.
 * 
 * <p>The bean may be treated as a singleton or not, depending on whether it should
 * allow for underlying refresh.</p>
 * 
 * @param <T> bean type
 */
public class ProxiedFactoryBean<T> extends AbstractFactoryBean<T> {
    
    /** Bean type. */
    @Nonnull private final Class<T> beanType; 

    /** Bean name. */
    @Nullable @NotEmpty private String beanName;

    /** Backup bean name. */
    @Nullable @NotEmpty private String backupName;

    /** Bean source. */
    @Nonnull private final ReloadableService<ApplicationContext> contextService;
    
    /**
     * Constructor.
     *
     * @param service bean source
     * @param type type of bean
     */
    public ProxiedFactoryBean(
            @Nonnull @ParameterName(name="service") final ReloadableService<ApplicationContext> service,
            @Nonnull @ParameterName(name="type") final Class<T> type) {
        
        beanType = Constraint.isNotNull(type, "Bean type cannot be null");
        contextService = Constraint.isNotNull(service, "FlowDescriptorResolver component cannot be null");
    }

    /**
     * Constructor.
     *
     * @param service bean source
     * @param type type of bean
     * @param name name of bean
     */
    public ProxiedFactoryBean(
            @Nonnull @ParameterName(name="service") final ReloadableService<ApplicationContext> service,
            @Nonnull @ParameterName(name="type") final Class<T> type,
            @Nonnull @NotEmpty @ParameterName(name="name") final String name) {
        this(service, type);
        beanName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Bean name cannot be null or empty");
    }

    /**
     * Constructor.
     *
     * @param service bean source
     * @param type type of bean
     * @param name name of bean
     * @param backup backup name in case primary doesn't exist
     */
    public ProxiedFactoryBean(
            @Nonnull @ParameterName(name="service") final ReloadableService<ApplicationContext> service,
            @Nonnull @ParameterName(name="type") final Class<T> type,
            @Nonnull @NotEmpty @ParameterName(name="name") final String name,
            @Nonnull @NotEmpty @ParameterName(name="backup") final String backup) {
        this(service, type);
        beanName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Bean name cannot be null or empty");
        backupName = Constraint.isNotNull(StringSupport.trimOrNull(backup), "Backup bean name cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Override
    public Class<T> getObjectType() {
        return beanType;
    }

    /** {@inheritDoc} */
    @Override
    protected T createInstance() throws Exception {
        
        final ServiceableComponent<ApplicationContext> component = contextService.getServiceableComponent();
        if (component == null) {
            throw new BeanCreationException("ApplicationContext not available");
        }
        
        try {
            if (beanName != null) {
                try {
                    final T bean = component.getComponent().getBean(beanName, beanType); 
                    if (bean != null) {
                        return bean;
                    } else if (backupName != null) {
                        return component.getComponent().getBean(backupName, beanType);
                    } else {
                        return null;
                    }
                } catch (final NoSuchBeanDefinitionException e) {
                    if (backupName != null) {
                        return component.getComponent().getBean(backupName, beanType);
                    }
                    throw e;
                }
            } else {
                return component.getComponent().getBean(beanType);
            }
        } finally {
            component.unpinComponent();
        }
    }
   
}