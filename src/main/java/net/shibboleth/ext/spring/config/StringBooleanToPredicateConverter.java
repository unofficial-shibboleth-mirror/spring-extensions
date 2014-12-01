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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Allows setting of fixed {@link Predicate} properties using a boolean string.
 */
public class StringBooleanToPredicateConverter implements Converter<String,Predicate> {

    /** {@inheritDoc} */
    public Predicate convert(String source) {
        return Boolean.valueOf(source) ? Predicates.alwaysTrue() : Predicates.alwaysFalse();
    }
    
}