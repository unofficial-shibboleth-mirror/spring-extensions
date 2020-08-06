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

import java.io.IOException;

import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.HttpClientContextHandler;
import net.shibboleth.utilities.java.support.repository.RepositorySupport;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for {@link ConditionalResource}.
 */
@SuppressWarnings("javadoc")
public class ConditionalResourceTest {

    private final String existsURL = RepositorySupport.buildHTTPResourceURL("spring-extensions", "src/test/resources/net/shibboleth/ext/spring/resource/document.xml",false);

    private final String nonExistsURL = RepositorySupport.buildHTTPResourceURL("spring-extensions.git", "trunk/src/test/resources/data/document.xml",false);

    private HttpClient client;

    @BeforeClass public void setupClient() throws Exception {
        client = (new HttpClientBuilder()).buildClient();
    }

    @Test public void existsTest() throws IOException, ComponentInitializationException {
        final HTTPResource existsHTTPResource = new HTTPResource(client, existsURL);
        final HTTPResource notExistsHTTPResource = new HTTPResource(client, nonExistsURL);
        
        final ConditionalResource existsResource = new ConditionalResource(existsHTTPResource);
        final ConditionalResource notExistsResource = new ConditionalResource(notExistsHTTPResource);
        
        existsResource.setId("test");
        existsResource.initialize();

        notExistsResource.setId("test");
        notExistsResource.initialize();

        Assert.assertTrue(existsHTTPResource.exists());
        Assert.assertFalse(notExistsHTTPResource.exists());

        Assert.assertTrue(existsResource.exists());
        Assert.assertTrue(notExistsResource.exists());
    }
    
    
    @Test public void contextHandlerFailBeforeTest() throws IOException, ComponentInitializationException {
        final HTTPResource existsHTTPResource = new HTTPResource(client, existsURL);
        existsHTTPResource.setHttpClientContextHandler(new HttpClientContextHandler() {
            public void invokeBefore(HttpClientContext context, HttpUriRequest request) throws IOException {
                throw new IOException("Fail");
            }
            public void invokeAfter(HttpClientContext context, HttpUriRequest request) throws IOException {
            }
        });
        
        final ConditionalResource existsResource = new ConditionalResource(existsHTTPResource);
        existsResource.setId("test");
        existsResource.initialize();

        Assert.assertTrue(existsResource.exists());
    }
    
    @Test public void contextHandlerFailAfterTest() throws IOException, ComponentInitializationException {
        final HTTPResource existsHTTPResource = new HTTPResource(client, existsURL);
        existsHTTPResource.setHttpClientContextHandler(new HttpClientContextHandler() {
            public void invokeBefore(HttpClientContext context, HttpUriRequest request) throws IOException {
            }
            public void invokeAfter(HttpClientContext context, HttpUriRequest request) throws IOException {
                throw new IOException("Fail");
            }
        });

        final ConditionalResource existsResource = new ConditionalResource(existsHTTPResource);
        existsResource.setId("test");
        existsResource.initialize();
        
        Assert.assertTrue(existsResource.exists());
    }

    @Test public void testCompare() throws IOException, ComponentInitializationException {

        final HTTPResource existsHTTPResource = new HTTPResource(client, existsURL);
        
        final ConditionalResource existsResource = new ConditionalResource(existsHTTPResource);
        existsResource.setId("test");
        existsResource.initialize();

        Assert.assertTrue(ResourceTestHelper.compare(existsResource, new ClassPathResource(
                "net/shibboleth/ext/spring/resource/document.xml")));
    }
    
    @Test public void testBeanExists() throws ComponentInitializationException {
        final ClassPathResource existsCPResource =
                new ClassPathResource("net/shibboleth/ext/spring/resource/conditional.xml");
        final ConditionalResource existsResource = new ConditionalResource(existsCPResource);
        existsResource.setId("test");
        existsResource.initialize();
        
        final GenericApplicationContext parentContext = new GenericApplicationContext();
        parentContext.refresh(); // THIS IS REQUIRED

        final GenericApplicationContext context = new GenericApplicationContext(parentContext);
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(existsResource);
        context.refresh();
        
        Assert.assertEquals(context.getBean("testBean"), "foo");
    }
    
    @Test public void testBeanMissing() throws ComponentInitializationException {
        final ClassPathResource missingCPResource =
                new ClassPathResource("net/shibboleth/ext/spring/resource/missing.xml");
        final ConditionalResource missingResource = new ConditionalResource(missingCPResource);
        missingResource.setId("test");
        missingResource.initialize();
        
        final GenericApplicationContext parentContext = new GenericApplicationContext();
        parentContext.refresh(); // THIS IS REQUIRED

        final GenericApplicationContext context = new GenericApplicationContext(parentContext);
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(missingResource);
        context.refresh();
        
        Assert.assertFalse(context.containsBean("testBean"));
    }
    
    @Test public void testImport() {
        final ClassPathResource resource =
                new ClassPathResource("net/shibboleth/ext/spring/resource/conditional-import.xml");
        
        final GenericApplicationContext parentContext = new GenericApplicationContext();
        parentContext.refresh(); // THIS IS REQUIRED

        final GenericApplicationContext context = new GenericApplicationContext(parentContext);
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);
        context.addProtocolResolver(new ConditionalResourceResolver());

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(resource);
        context.refresh();
        
        Assert.assertEquals(context.getBean("testBean"), "foo");
    }
    
}