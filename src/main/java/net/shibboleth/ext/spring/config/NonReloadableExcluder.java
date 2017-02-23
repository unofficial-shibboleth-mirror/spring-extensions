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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Post-processes bean definitions by marking any reloadable beans as singletons,
 * and any non-reloadable beans as lazy-init to limit/prevent instantiation.
 * 
 * <p>Used to implement the "reloadable" custom bean scope in concert with {@link ReloadableScope}.</p>
 * 
 * @since 5.4.0
 */
public class NonReloadableExcluder implements BeanFactoryPostProcessor {
    
    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(NonReloadableExcluder.class);

    /** {@inheritDoc} */
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        
        for (final String beanName : beanFactory.getBeanDefinitionNames()) {
            final BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
            if (beanDef.getScope() != null && ReloadableScope.SCOPE_RELOADABLE.equals(beanDef.getScope())) {
                log.debug("Converting reloadable bean '{}' into singleton", beanName);
                beanDef.setScope(BeanDefinition.SCOPE_SINGLETON);
            } else {
                log.debug("Hiding non-reloadable bean '{}' as a lazy-init", beanName);
                beanDef.setLazyInit(true);
            }
        }
    }

}