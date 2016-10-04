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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.ParameterName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * An implementation of {@link ParameterNameDiscoverer} that is driven by the {@link ParameterName} Annotation.
 *
 * @since 5.3.0 (IdP 3.3.0)
 */
public class AnnotationParameterNameDiscoverer extends DefaultParameterNameDiscoverer implements
        ParameterNameDiscoverer {

    /** log. */
    private final Logger log = LoggerFactory.getLogger(AnnotationParameterNameDiscoverer.class);

    /** {@inheritDoc} */
    @Override @Nullable public String[] getParameterNames(final Method method) {

        return super.getParameterNames(method);
    }

    /** Given the annotations for each parameter is it one of ours?
     * @param annotations the annotations for the parameter
     * @return the "name" if one of ours. null otherwise
     */
    @Nullable private String getMyAnnotation(final Annotation[] annotations) {
        for (final Annotation a : annotations) {
            if (a instanceof ParameterName) {
                final ParameterName param = (ParameterName) a;
                return param.name();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc} <br/>
     * If we cannot do anything pass to the default discoverer.
     */
    @Override public String[] getParameterNames(final Constructor<?> ctor) {

        final Annotation[][] annotationsArray = ctor.getParameterAnnotations();
        if (annotationsArray.length == 0) {
            // nothing to do
            return super.getParameterNames(ctor);
        }

        final String className = ctor.getDeclaringClass().getName();
        final boolean isOurs = (className != null) && 
                (className.startsWith("org.opensaml") || className.startsWith("net.shibboleth"));
        if (!isOurs) {
            return super.getParameterNames(ctor);
        }

        boolean allPresent = true;
        final String[] names = new String[annotationsArray.length];

        for (int index = 0; index < annotationsArray.length; index++) {
            names[index] = getMyAnnotation(annotationsArray[index]);
            if (names[index] == null) {
                allPresent = false;
            }
        }

        if (!allPresent) {

            log.debug("Constructor for class '{}' with {} parameters: "
                    + "Not all parameters are annotated with @ParameterName", className, annotationsArray.length);
            log.debug("Types : {}", (Object)ctor.getParameterTypes());
            return super.getParameterNames(ctor);
        }
        log.trace("Constructor for class '{}' with {} parameters called {}", className, names.length, names);
        return names;
    }
}
