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

import java.io.IOException;
import java.util.Collections;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ReloadableBeanServiceTest {

    @Test public void reloadableService() throws IOException, InterruptedException {
        
        final GenericApplicationContext appCtx = new ApplicationContextBuilder()
                .setName("appCtx")
                .setServiceConfigurations(Collections.<Resource>singletonList(
                        new ClassPathResource("net/shibboleth/ext/spring/service/ReloadableBeans1.xml")))
                .build();

        try {
            final NonReloadableTestBean bean = appCtx.getBean("nonReloadableBean", NonReloadableTestBean.class);
            Assert.assertEquals(10, bean.getValue());
            
            final ReloadableTestBean child1 = bean.getChild();
            
            final ReloadableService embedded = (ReloadableService) appCtx.getBean("reloadableBeanService");
            
            final ServiceableComponent<ApplicationContext> component = embedded.getServiceableComponent();
            try {
                Assert.assertFalse(component.getComponent().containsLocalBean("reloadableBeanService"));
            } finally {
                component.unpinComponent();
            }
            
            embedded.reload();

            final ReloadableTestBean child2 = bean.getChild();

            Assert.assertNotSame(child1, child2);
            
        } finally {
            appCtx.close();
        }
    }

}