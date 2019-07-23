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

import org.springframework.core.convert.converter.Converter;

import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;

/**
 * Auto-converts standard Java predicates to Guava's version for legacy compatibility.
 * 
 * @param <T> input type
 */
public class PredicateToPredicateConverter<T>
        implements Converter<java.util.function.Predicate<T>,com.google.common.base.Predicate<T>> {

    /** {@inheritDoc} */
    public com.google.common.base.Predicate<T> convert(final java.util.function.Predicate<T> source) {
        DeprecationSupport.warn(ObjectType.CLASS, com.google.common.base.Predicate.class.getName(), null,
                java.util.function.Predicate.class.getName());
        return source::test;
    }
    
}