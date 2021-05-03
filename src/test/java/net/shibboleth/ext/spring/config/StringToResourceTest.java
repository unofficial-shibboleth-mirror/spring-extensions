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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.resource.Resource;

@SuppressWarnings("javadoc")
public class StringToResourceTest {

    private final StringToResourceConverter converter = new StringToResourceConverter();

    private String path;

    @BeforeClass public void setup() throws IOException {
        final File file = File.createTempFile("TEST", "convert");
        file.createNewFile();
        file.deleteOnExit();
        assertTrue(file.exists());
        path = file.getAbsolutePath();
    }

    @Test public void exists() {
        final Resource r = converter.convert(path);
        assertTrue(r.exists());
    }

    @Test public void notExist() {
        final Resource r = converter.convert(path + "x");
        assertFalse(r.exists());
    }

    @Test public void endsWithSpace() {
        try {
            final Resource r = converter.convert(path + " ");
            assertFalse(r.exists()); // Linux
        } catch (final InvalidPathException e) {
            // expected on Windows
        }
    }
}
