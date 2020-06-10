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

package net.shibboleth.ext.spring.util;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A utility bean to look for and warn about deprecated bean names.
 * 
 * <p>In practice this is difficult to use effectively because it has to be run only
 * after all other beans are in place, so it's best-suited to injection into application
 * workflows such that it's deliberately used at a known point in the process.</p>
 * 
 * @since 6.1.0
 */
public class DeprecatedBeanDetector implements ApplicationListener<ContextRefreshedEvent> {

    /** Context for log warnings. */
    @Nullable private final String warnContext;
    
    /** Deprecated bean names. */
    @Nonnull @NonnullElements private final Map<String,String> beanNames; 
    
    /**
     * Constructor.
     *
     * @param map bean names to detect with replacements identified
     * @param logContext context for log warnings
     */
    public DeprecatedBeanDetector(@Nonnull final Map<String,String> map, @Nullable final String logContext) {
        
        beanNames = new HashMap<>(map.size());
        map.forEach((k,v) -> {
            final String key = StringSupport.trimOrNull(k);
            final String val = StringSupport.trimOrNull(v);
            if (key != null) {
                beanNames.put(key, val);
            }
        });
        
        warnContext = logContext;
    }

    /** {@inheritDoc} */
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        
        final ApplicationContext applicationContext = (ApplicationContext) event.getSource();
        beanNames.forEach((k,v) -> {
            if (applicationContext.containsLocalBean(k)) {
                DeprecationSupport.warn(ObjectType.BEAN, k, warnContext, v);
            }
        });
    }

}