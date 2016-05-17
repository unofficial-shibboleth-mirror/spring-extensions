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

package net.shibboleth.ext.spring.context;

import javax.annotation.Nonnull;

import org.springframework.util.StringUtils;

/**
 * Version of {@link DeferPlaceholderFileSystemXmlWebApplicationContext} which does not necessarily assume that file
 * list are space separated.
 */
public class DelimiterAwareApplicationContext extends DeferPlaceholderFileSystemXmlWebApplicationContext {

    /** {@inheritDoc} */
    @Override public void setConfigLocation(final String location) {
        setConfigLocations(StringUtils.tokenizeToStringArray(location, getDelimiters()));
    }

    /**
     * Get the delimiters we will split the config locations on. All the usual suspects except space.
     * 
     * @return the delimiters
     */
    @Nonnull protected String getDelimiters() {
        return ",;\t\n";
    }
}
