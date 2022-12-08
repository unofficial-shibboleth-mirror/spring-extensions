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

import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.Test;

/**
 * Test for JSSH-20, remote entity access by Spring.
 */
public class CanarySchemaTest {

    @Test(expectedExceptions=XmlBeanDefinitionStoreException.class)
    void Test() {
        final GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext for Canary");

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        // This should throw XmlBeanDefinitionStoreException due to the underlying attempt to resolve the AFP schema
        // as an import. If the bug still existed or manifests differently, the schema will be fetched directly from
        // shibboleth.net and the import will work.
        
        // Note that transitory issues with shibboleth.net should be ok here. While they would mask things such that the
        // bug might exist again but the test "fail" due to shibboleth.net being down, that shouldn't persist long
        // enough and we'd catch it eventually "working".
        
        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource("/net/shibboleth/ext/spring/util/canary.xml"));
        context.refresh();
    }
    
}