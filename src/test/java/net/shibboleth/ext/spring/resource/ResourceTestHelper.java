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

package net.shibboleth.ext.spring.resource;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

import com.google.common.io.Closeables;

/**
 *
 */
public class ResourceTestHelper {

    static public boolean compare(final Resource first, final Resource second) throws IOException {
        final InputStream firstStream = first.getInputStream();
        final InputStream secondStream = second.getInputStream();

        try {
            while (true) {

                // Remove any differences based on CRLF handling
                int firstInt = firstStream.read();
                while (firstInt == 10 || firstInt == 13) {
                    firstInt = firstStream.read();
                }
                int secondInt = secondStream.read();
                while (secondInt == 10 || secondInt == 13) {
                    secondInt = secondStream.read();
                }

                if (firstInt == -1) {
                    return secondInt == -1;
                }

                if (firstInt != secondInt) {
                    return false;
                }
            }
        } finally {
            Closeables.close(firstStream, true);
            Closeables.close(secondStream, true);
        }
    }

}
