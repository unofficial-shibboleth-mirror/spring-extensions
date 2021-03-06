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

import java.time.Duration;

import net.shibboleth.utilities.java.support.xml.DOMTypeSupport;

import org.springframework.core.convert.converter.Converter;

/**
 * Allows setting of Duration-valued properties using lexical string form.
 */
public class StringToDurationConverter implements Converter<String,Duration> {

    /** {@inheritDoc} */
    public Duration convert(final String source) {
        if (source.startsWith("P") || source.startsWith("-P")) {
            return DOMTypeSupport.stringToDuration(source.trim());
        }
        
        // Treat as milliseconds.
        return Duration.ofMillis(Long.valueOf(source));
    }

}