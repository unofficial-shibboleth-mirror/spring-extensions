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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.core.convert.converter.Converter;

import net.shibboleth.utilities.java.support.ddf.DDF;

/**
 * Spring converter from byte array to {@link DDF} object.
 * 
 * @since 7.0.0
 */
public class ByteArrayToDDFConverter implements Converter<byte[], DDF> {

    /** {@inheritDoc} */
    public DDF convert(final byte[] source) {
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(source)) {
            return DDF.deserialize(bais);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}