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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collection;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.Test;

public class NestedParsersTest {

    @Test void Test() {
        final GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext for Nested ");

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource("/net/shibboleth/ext/spring/util/nested.xml"));
        context.refresh();

        final Collection<UpperParserAndBean> uppers = context.getBeansOfType(UpperParserAndBean.class).values();
        final Collection<LowerParsersAndBean> lowers = context.getBeansOfType(LowerParsersAndBean.class).values();
        
        assertEquals(uppers.size(), 2);
        
        assertEquals(lowers.size(), 1);
        assertEquals(lowers.iterator().next().getMessage(), "first");

        assertTrue(uppers.stream().allMatch(x -> "2ndFirst".equals(x.getMessage()) || "2ndSecond".equals(x.getMessage()))); 
    }
    
}
