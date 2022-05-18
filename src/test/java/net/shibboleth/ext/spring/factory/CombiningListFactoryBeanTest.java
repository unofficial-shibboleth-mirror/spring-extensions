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

package net.shibboleth.ext.spring.factory;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;

@SuppressWarnings("javadoc")
public class CombiningListFactoryBeanTest {

    @Test
    public void test() {
        
        final Resource r = new ClassPathResource("net/shibboleth/ext/spring/factory/lists.xml");
  
        final GenericApplicationContext ctx = new ApplicationContextBuilder()
                .setName("appCtx")
                .setServiceConfigurations(Collections.singletonList(r))
                .build();
        
        
        final List<?> list = (List<?>) ctx.getBean("combined");
        
        assertEquals(list.size(), 4);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains("parent"));
        assertTrue(list.contains("child"));
        
        final List<?> empty = (List<?>) ctx.getBean("nulls");
        assertEquals(empty.size(), 0);
    }
    
    @Test
    public void resourceTest() {
        
        final Resource r = new ClassPathResource("net/shibboleth/ext/spring/factory/resourceLists.xml");
  
        final GenericApplicationContext ctx = new ApplicationContextBuilder()
                .setName("appCtx")
                .setServiceConfigurations(Collections.singletonList(r))
                .build();
        
        
        final ResourceListBean bean = ctx.getBean(ResourceListBean.class);
        
        assertEquals(bean.getResources().size(), 2);
        
    }

}