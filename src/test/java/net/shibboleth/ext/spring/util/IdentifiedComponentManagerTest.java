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

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;

/**
 * Test for {@link IdentifiedComponentManager}.
 */
@SuppressWarnings("javadoc")
public class IdentifiedComponentManagerTest {

    @Test public void testCombining() {
        final ApplicationContext context = getContext("net/shibboleth/ext/spring/util/combined.xml");
        
        final MockComponentManager manager = context.getBean(MockComponentManager.class);
        Assert.assertNotNull(manager);
        Assert.assertEquals(manager.getComponents().size(), 3);
        
        Iterator<MockComponent> i = manager.getComponents().iterator();
        Assert.assertEquals(i.next().getId(), "One");
        Assert.assertEquals(i.next().getId(), "Two");
        Assert.assertEquals(i.next().getId(), "Three");

        i = manager.getComponents().iterator();
        Assert.assertEquals(i.next().getData(), "Foo1");
        Assert.assertEquals(i.next().getData(), "Foo2");
        Assert.assertEquals(i.next().getData(), "Foo3");
    }

    @Test public void testOverlap() {
        final ApplicationContext context = getContext("net/shibboleth/ext/spring/util/overlap.xml");
        
        final MockComponentManager manager = context.getBean(MockComponentManager.class);
        Assert.assertNotNull(manager);
        Assert.assertEquals(manager.getComponents().size(), 3);
        
        Iterator<MockComponent> i = manager.getComponents().iterator();
        Assert.assertEquals(i.next().getId(), "One");
        Assert.assertEquals(i.next().getId(), "Three");
        Assert.assertEquals(i.next().getId(), "Two");

        i = manager.getComponents().iterator();
        Assert.assertEquals(i.next().getData(), "Foo1");
        Assert.assertEquals(i.next().getData(), "Foo3");
        Assert.assertEquals(i.next().getData(), "Foo2");
    }

    @Test public void testStaticOnly() {
        final ApplicationContext context = getContext("net/shibboleth/ext/spring/util/staticOnly.xml");
        
        final MockComponentManager manager = context.getBean(MockComponentManager.class);
        Assert.assertNotNull(manager);
        Assert.assertEquals(manager.getComponents().size(), 3);
        
        Iterator<MockComponent> i = manager.getComponents().iterator();
        Assert.assertEquals(i.next().getId(), "One");
        Assert.assertEquals(i.next().getId(), "Two");
        Assert.assertEquals(i.next().getId(), "Three");

        i = manager.getComponents().iterator();
        Assert.assertEquals(i.next().getData(), "Foo1");
        Assert.assertEquals(i.next().getData(), "Foo2");
        Assert.assertEquals(i.next().getData(), "Foo3");
    }

    @Test public void testFreeOnly() {
        final ApplicationContext context = getContext("net/shibboleth/ext/spring/util/freeOnly.xml");
        
        final MockComponentManager manager = context.getBean(MockComponentManager.class);
        Assert.assertNotNull(manager);
        Assert.assertEquals(manager.getComponents().size(), 1);
        
        Iterator<MockComponent> i = manager.getComponents().iterator();
        Assert.assertEquals(i.next().getId(), "Three");

        i = manager.getComponents().iterator();
        Assert.assertEquals(i.next().getData(), "Foo3");
    }

    private ApplicationContext getContext(final String config) {
        final GenericApplicationContext context = new GenericApplicationContext();
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(config);
        context.refresh();
     
        return context;
    }

    public static class MockComponent extends AbstractIdentifiableInitializableComponent {

        private final String data;
        
        public MockComponent(final String s) {
            data = s;
        }
        
        public String getData() {
            return data;
        }
        
        /** {@inheritDoc} */
        @Override public int hashCode() {
            return getId().hashCode();
        }

        /** {@inheritDoc} */
        @Override public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj == this) {
                return true;
            }

            if (obj instanceof MockComponent) {
                return getId().equals(((MockComponent) obj).getId());
            }

            return false;
        }
    }
    
    public static class MockComponentManager extends IdentifiedComponentManager<MockComponent> {
        /**
         * Constructor.
         *
         * @param freeObjects
         */
        @Autowired
        public MockComponentManager(@Nonnull @NonnullElements final Collection<MockComponent> freeObjects) {
            super(freeObjects);
        }
    }

}