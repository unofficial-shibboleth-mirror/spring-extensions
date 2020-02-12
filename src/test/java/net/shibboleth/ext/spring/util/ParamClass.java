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

import net.shibboleth.utilities.java.support.annotation.ParameterName;

/**
 * simple bean.
 */
@SuppressWarnings("javadoc")
public class ParamClass {

    private final String p1;
    private final String p2;
    
    public ParamClass(@ParameterName(name="param1") final String param2, @ParameterName(name="param2") final String param1) {
        p1 = param2;
        p2 = param1;
    }
    
    public ParamClass(final String param1) {
        p1 = param1;
        p2 = "HardWired Param The Second";
    }

    public String getP1() {return p1;}
    public String getP2() {return p2;}
}
