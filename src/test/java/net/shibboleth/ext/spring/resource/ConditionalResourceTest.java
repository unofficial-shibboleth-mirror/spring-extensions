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
public class ConditionalResourceTest {

    private final String existsURL =
            "https://git.shibboleth.net/view/?p=spring-extensions.git;a=blob_plain;f=src/test/resources/data/document.xml;h=e8ec7c0d20c7a6b8193e1868398cda0c28df45ed;hb=HEAD";

    private final String nonExistsURL =
            "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/documxent.xml?view=co";

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
    
    @Test public void testBeanExists() {
        final ClassPathResource existsCPResource =
                new ClassPathResource("net/shibboleth/ext/spring/resource/conditional.xml");
        final ConditionalResource existsResource = new ConditionalResource(existsCPResource);
        
        final GenericApplicationContext parentContext = new GenericApplicationContext();
        parentContext.refresh(); // THIS IS REQUIRED

        final GenericApplicationContext context = new GenericApplicationContext(parentContext);
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(existsResource);
        context.refresh();
        
        Assert.assertEquals(context.getBean("testBean"), "foo");
    }
    
    @Test public void testBeanMissing() {
        final ClassPathResource missingCPResource =
                new ClassPathResource("net/shibboleth/ext/spring/resource/missing.xml");
        final ConditionalResource missingResource = new ConditionalResource(missingCPResource);
        
        final GenericApplicationContext parentContext = new GenericApplicationContext();
        parentContext.refresh(); // THIS IS REQUIRED

        final GenericApplicationContext context = new GenericApplicationContext(parentContext);
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(missingResource);
        context.refresh();
        
        Assert.assertFalse(context.containsBean("testBean"));
    }
    
}