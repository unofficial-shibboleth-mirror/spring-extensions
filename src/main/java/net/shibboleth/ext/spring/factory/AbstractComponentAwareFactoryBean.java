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

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.InitializableComponent;

/**
 * A Factory bean which is aware of the component lifecycle interfaces.
 * 
 * @param <T> The type to implement
 */
public abstract class AbstractComponentAwareFactoryBean<T> extends AbstractFactoryBean<T> {

    /** {@inheritDoc}. Call our destroy method if aposite. */
    @Override protected void destroyInstance(final T instance) throws Exception {
        super.destroyInstance(instance);
        if (instance instanceof DestructableComponent) {
            ((DestructableComponent) instance).destroy();
        }
    }

    /**
     * Call the parent class to create the object, then initialize it aposite. {@inheritDoc}.
     */
    @Override protected final T createInstance() throws Exception {
        if (!isSingleton()) {
            LoggerFactory.getLogger(AbstractComponentAwareFactoryBean.class).error(
                    "Configuration error: {} should not be used to create prototype beans."
                            + "  Destroy is never called for prototype beans", AbstractComponentAwareFactoryBean.class);
            throw new BeanCreationException("Do not use AbstractComponentAwareFactoryBean to create prototype beans");
        }
        final T theBean = doCreateInstance();
        if (theBean instanceof InitializableComponent) {
            ((InitializableComponent) theBean).initialize();
        }
        return theBean;
    }

    /**
     * Call the parent class to create the instance.
     * 
     * @return the bean.
     * @throws Exception if needed.
     */
    protected abstract T doCreateInstance() throws Exception;
}
