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

package net.shibboleth.ext.spring.service;

import javax.annotation.Nonnull;

@SuppressWarnings("javadoc")
public class TestServiceableComponent extends AbstractServiceableComponent<TestServiceableComponent> {

    private String theValue;
    
    /** {@inheritDoc} */
    @Override
    @Nonnull public TestServiceableComponent getComponent() {
        return this;
    }

    /**
     * @return Returns the theValue.
     */
    public String getTheValue() {
        return theValue;
    }

    /**
     * @param value The theValue to set.
     */
    public void setTheValue(final String value) {
        theValue = value;
    }

}