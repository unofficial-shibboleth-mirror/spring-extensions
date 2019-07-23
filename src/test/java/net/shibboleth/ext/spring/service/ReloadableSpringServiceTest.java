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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.google.common.io.ByteStreams;

/** {@link ReloadableSpringService} unit test. */
public class ReloadableSpringServiceTest {

    private static final Duration RELOAD_DELAY = Duration.ofMillis(100);

    private File testFile;

    private void createPopulatedFile(final String dataPath) throws IOException {
        testFile = File.createTempFile("ReloadableSpringServiceTest", ".xml");
        overwriteFileWith(dataPath);
        testFile.setLastModified(365*24*60*60*1000);
    }
    
    @AfterMethod public void  deleteFile() {
        if (null != testFile) {
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }

    private Resource testFileResource() {
        return new FileSystemResource(testFile);
    }

    private void overwriteFileWith(final String newDataPath) throws IOException {
        final OutputStream stream = new FileOutputStream(testFile);
        ByteStreams.copy(new ClassPathResource(newDataPath).getInputStream(), stream);
        stream.close();
    }

    @Test(enabled=true) public void reloadableService() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/ext/spring/service/ServiceableBean1.xml");

        service.setFailFast(true);
        service.setId("reloadableService");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        service.start();

        ServiceableComponent<TestServiceableComponent> serviceableComponent = service.getServiceableComponent();
        final TestServiceableComponent component = serviceableComponent.getComponent();

        Assert.assertEquals("One", component.getTheValue());
        Assert.assertFalse(component.getComponent().isDestroyed());

        serviceableComponent.unpinComponent();
        overwriteFileWith("net/shibboleth/ext/spring/service/ServiceableBean2.xml");

        long count = 70;
        while (count > 0 && !component.isDestroyed()) {
            Thread.sleep(RELOAD_DELAY.toMillis());
            count--;
        }
        Assert.assertTrue(component.isDestroyed(), "After 7 second initial component has still not be destroyed");

        //
        // The reload will have destroyed the old component
        //
        Assert.assertTrue(serviceableComponent.getComponent().isDestroyed());

        serviceableComponent = service.getServiceableComponent();

        Assert.assertEquals(serviceableComponent.getComponent().getTheValue(), "Two");
        serviceableComponent.unpinComponent();
        service.stop();
        
        testFile.delete();
    }

    @Test(enabled=true) public void deferedReload() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/ext/spring/service/ServiceableBean1.xml");

        service.setFailFast(true);
        service.setId("deferedReload");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        service.start();

        ServiceableComponent<TestServiceableComponent> serviceableComponent = service.getServiceableComponent();
        final TestServiceableComponent component = serviceableComponent.getComponent();
        
        final Instant x = service.getLastReloadAttemptInstant();
        Assert.assertEquals(x,  service.getLastSuccessfulReloadInstant());

        Assert.assertEquals(component.getTheValue(), "One");
        Assert.assertFalse(component.isDestroyed());

        Thread.sleep(RELOAD_DELAY.toMillis() * 3);
        Assert.assertEquals(x,  service.getLastReloadAttemptInstant());

        overwriteFileWith("net/shibboleth/ext/spring/service/ServiceableBean2.xml");

        //
        // The reload will not have destroyed the old component yet
        //
        Assert.assertFalse(component.isDestroyed());

        long count = 70;
        TestServiceableComponent component2 = null;
        while (count > 0) {
            serviceableComponent = service.getServiceableComponent();
            component2 = serviceableComponent.getComponent();
            if ("Two".equals(component2.getTheValue())) {
                component2.unpinComponent();
                break;
            }
            component2.unpinComponent();
            component2 = null;
            Thread.sleep(RELOAD_DELAY.toMillis());
            count--;
        }
        Assert.assertNotNull(component2, "After 7 second initial component has still not got new value");

        component.unpinComponent();

        count = 70;
        while (count > 0 && !component.isDestroyed()) {
            Thread.sleep(RELOAD_DELAY.toMillis());
            count--;
        }
        Assert.assertTrue(component.isDestroyed(), "After 7 second initial component has still not be destroyed");

        service.stop();
        testFile.delete();
    }

    @Test public void testFailFast() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/ext/spring/service/BrokenBean1.xml");

        service.setFailFast(true);
        service.setId("testFailFast");
        service.setReloadCheckDelay(Duration.ZERO);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        try {
            service.start();
            Assert.fail("Expected to fail");
        } catch (final BeanInitializationException e) {
            // OK
        }
        Assert.assertNull(service.getServiceableComponent());

        overwriteFileWith("net/shibboleth/ext/spring/service/ServiceableBean2.xml");

        Thread.sleep(RELOAD_DELAY.toMillis() * 2);
        Assert.assertNull(service.getServiceableComponent());

        service.stop();
        testFile.delete();
    }

    @Test public void testNotFailFast() throws IOException, InterruptedException {
        final ReloadableSpringService<TestServiceableComponent> service =
                new ReloadableSpringService<>(TestServiceableComponent.class);

        createPopulatedFile("net/shibboleth/ext/spring/service/BrokenBean1.xml");

        service.setFailFast(false);
        service.setId("testNotFailFast");
        service.setReloadCheckDelay(RELOAD_DELAY);
        service.setServiceConfigurations(Collections.singletonList(testFileResource()));

        service.start();
        Assert.assertNull(service.getServiceableComponent());

        overwriteFileWith("net/shibboleth/ext/spring/service/ServiceableBean2.xml");

        long count = 700;
        ServiceableComponent<TestServiceableComponent> serviceableComponent = service.getServiceableComponent();
        while (count > 0 && null == serviceableComponent) {
            Thread.sleep(RELOAD_DELAY.toMillis());
            count--;
            serviceableComponent = service.getServiceableComponent();
        }
        Assert.assertNotNull(serviceableComponent, "After 7 second component has still no initialized");
        final TestServiceableComponent component = serviceableComponent.getComponent();
        Assert.assertEquals(component.getTheValue(), "Two");

        Assert.assertFalse(component.isDestroyed());
        component.unpinComponent();
        service.stop();

        count = 70;
        while (count > 0 && !component.isDestroyed()) {
            Thread.sleep(RELOAD_DELAY.toMillis());
            count--;
        }
        Assert.assertTrue(component.isDestroyed(), "After 7 seconds component has still not be destroyed");

        testFile.delete();
    }

    @Test public void testApplicationContextAware() {

        final Resource parentResource = new ClassPathResource("net/shibboleth/ext/spring/service/ReloadableSpringService.xml");

        final GenericApplicationContext appCtx = new ApplicationContextBuilder()
                .setName("appCtx")
                .setServiceConfigurations(Collections.singletonList(parentResource))
                .build();
        try {
            final ReloadableSpringService<?> service = appCtx.getBean("testReloadableSpringService", ReloadableSpringService.class);
    
            Assert.assertNotNull(service.getParentContext(), "Parent context should not be null");
        } finally {
            appCtx.close();
        }
    }
    
    @Test public void testBeanNameAware() {

        final Resource parentResource = new ClassPathResource("net/shibboleth/ext/spring/service/ReloadableSpringService.xml");

        final GenericApplicationContext appCtx = new ApplicationContextBuilder()
                .setName("appCtx")
                .setServiceConfigurations(Collections.singletonList(parentResource))
                .build();
        try {
            final ReloadableSpringService<?> service1 =
                    appCtx.getBean("testReloadableSpringService", ReloadableSpringService.class);
            Assert.assertEquals(service1.getId(), "testReloadableSpringService");

            final ReloadableSpringService<?> service2 =
                    appCtx.getBean("testReloadableSpringServiceWithCustomID", ReloadableSpringService.class);
            Assert.assertEquals(service2.getId(), "CustomID");
        } finally {
            appCtx.close();
        }
    }

}