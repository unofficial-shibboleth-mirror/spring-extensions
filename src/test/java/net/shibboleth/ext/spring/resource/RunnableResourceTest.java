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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.script.ScriptException;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;
import net.shibboleth.utilities.java.support.scripting.ScriptedRunnable;

/**
 *
 */
public class RunnableResourceTest {
    
    private String fileName;
    
    private CustomObject object;

    @BeforeClass public void setupClient() throws Exception {
        final File file = File.createTempFile("RunnableResourceTest", ".xml");
        fileName = file.getAbsolutePath();
        object = new CustomObject();
    }

    @AfterClass public void deleteFile() {
        final File f = new File(fileName);
        if (f.exists()) {
            f.delete();
        }
    }
    
    private byte getValue(final Resource resource) throws IOException {
        try (final InputStream io = resource.getInputStream()) {
            return (byte) io.read();
        }
    }
    
    @BeforeMethod public void reset() {
        object.reset();
    }
    
    @Test public void testCustomObject() throws IOException {
        Assert.assertFalse(object.wasUppdated());
        Assert.assertFalse(object.wasUppdated());
        Assert.assertTrue(object.isValid((byte) 0));
        Assert.assertFalse(object.isValid((byte) 1));
        object.update();
        Assert.assertTrue(object.wasUppdated());
        Assert.assertTrue(object.isValid((byte) 0));
        Assert.assertTrue(object.isValid((byte) 1));
        Assert.assertFalse(object.isValid((byte) 0));
    }
    
    
    @Test public void testRunnable() throws ScriptException, ComponentInitializationException {
        final EvaluableScript script = new EvaluableScript("custom.update();");
        final ScriptedRunnable runnable = new ScriptedRunnable();
        runnable.setCustomObject(object);
        runnable.setScript(script);
        runnable.setId("Runnable");
        runnable.initialize();
        
        Assert.assertFalse(object.wasUppdated());
        Assert.assertFalse(object.wasUppdated());
        Assert.assertTrue(object.isValid((byte) 0));
        Assert.assertFalse(object.isValid((byte) 1));
        runnable.run();
        Assert.assertTrue(object.wasUppdated());
        Assert.assertTrue(object.isValid((byte) 0));
        Assert.assertTrue(object.isValid((byte) 1));
        Assert.assertFalse(object.isValid((byte) 0));
        
    }
    
    @Test public void testResource() throws ScriptException, ComponentInitializationException, IOException, InterruptedException {
        
        final long now = new File(fileName).lastModified();
        final EvaluableScript script = new EvaluableScript("custom.update();");
        final ScriptedRunnable runnable = new ScriptedRunnable();
        runnable.setCustomObject(object);
        runnable.setScript(script);
        runnable.setId("Runnable");
        runnable.initialize();
    
        Assert.assertFalse(object.wasUppdated());
        Assert.assertFalse(object.wasUppdated());
        Assert.assertTrue(object.isValid((byte) 0));

        final Resource resource = new RunnableFileSystemResource(fileName, runnable);
        Assert.assertTrue(object.wasUppdated());
        Assert.assertTrue(object.isValid((byte) 1));
        Assert.assertTrue(object.isValid(getValue(resource))); // GetValue increments
        Assert.assertTrue(object.isValid((byte) 2));
        
        Assert.assertTrue(resource.exists()); // exists increments
        Assert.assertTrue(object.wasUppdated());
        Assert.assertTrue(object.isValid(getValue(resource))); // GetValue increments
        Assert.assertTrue(object.isValid((byte) 4));
        
        Thread.sleep(20);
        final long modified = resource.lastModified(); // lastModified Increments
        Assert.assertTrue(object.wasUppdated());
        Assert.assertTrue(object.isValid(getValue(resource)));// GetValue increments
        Assert.assertTrue(object.isValid((byte) 6));
        
        Assert.assertTrue(modified >= now);
        Thread.sleep(20);
        Assert.assertTrue(modified <= new File(fileName).lastModified());
        
    }
    
    @Test(enabled=true) public void testSpringLoad() {

        final GenericApplicationContext context = new GenericApplicationContext();
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions("classpath:net/shibboleth/ext/spring/resource/RunnableResource.xml");

        context.refresh();
        final Collection<Resource> beans = context.getBeansOfType(Resource.class).values();
        Assert.assertEquals(beans.size(), 1);    
    }
    
    
    
    
    public class CustomObject {
        
        private final File theFile;
        
        private byte count, lastCheck;
        
        private boolean updated;
        
        public CustomObject() {
            theFile = new File(fileName);
        }
        
        public void update() throws IOException {
            try (final FileOutputStream io = new FileOutputStream(theFile)) {
                io.write(count);
                
                if (count == 127 ) {
                    throw new IOException("Count wrapped"); 
                }
                count++;
                updated = true;
            }
        }
        
        public boolean isValid(final byte what) {
            if(what >= lastCheck && what <= count) {
                lastCheck = what;
                return true;
            }
            return false;
        }
        
        public boolean wasUppdated() {
            final boolean result = updated;
            updated = false;
            return result;
        }
        
        public void reset() {
            updated = false;
            count = 0;
            lastCheck = 0;
        }
    }

}
