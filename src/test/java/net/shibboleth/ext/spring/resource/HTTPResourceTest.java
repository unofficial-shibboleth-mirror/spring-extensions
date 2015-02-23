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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.InMemoryCachingHttpClientBuilder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.CacheResponseStatus;
import org.joda.time.DateTime;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for HTTPResource.
 */
public class HTTPResourceTest {

    private final String existsURL =
            "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/document.xml?view=co";

    private final String nonExistsURL =
            "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/documxent.xml?view=co";

    private HttpClient client;

    @BeforeClass public void setupClient() throws Exception {
        client = (new HttpClientBuilder()).buildClient();
    }

    @Test public void existsTest() throws IOException {
        HTTPResource existsResource = new HTTPResource(client, existsURL);
        HTTPResource notExistsResource = new HTTPResource(client, nonExistsURL);

        Assert.assertTrue(existsResource.exists());
        Assert.assertFalse(notExistsResource.exists());
    }

    @Test public void testCompare() throws IOException {

        Assert.assertTrue(ResourceTestHelper.compare(new HTTPResource(client, existsURL), new ClassPathResource(
                "data/document.xml")));
    }

    @Test public void testRelated() throws IOException {

        // Chose a file unlikely to change. Do not use the svn thing because the date will not be there

        final HTTPResource parent =
                new HTTPResource(client, "http://shibboleth.net/downloads/identity-provider/2.0.0/");
        final HTTPResource child = parent.createRelative("shibboleth-idp-2.0.0-bin.zip");

        final long when = child.lastModified();
        final long size = child.contentLength();
        final String whenAsString = new DateTime(when).toString();

        Assert.assertEquals(when, 1205848652000L, "Expected date of " + whenAsString + " did not match)");
        Assert.assertEquals(size, 20784226L, "Size mismatch");
    }

    @Test public void testCachedNoCache() throws IOException, InterruptedException {

        TestHTTPResource what = new TestHTTPResource(client, existsURL);
        Assert.assertTrue(what.exists());
        Assert.assertNull(what.getLasteCacheResponseStatus());
        Assert.assertTrue(ResourceTestHelper.compare(what, new ClassPathResource("data/document.xml")));
        Assert.assertNull(what.getLasteCacheResponseStatus());
    }

    @Test public void testCachedCache() throws Exception {

        final InMemoryCachingHttpClientBuilder builder = new InMemoryCachingHttpClientBuilder();
        builder.setMaxCacheEntries(3);
        TestHTTPResource what = new TestHTTPResource(builder.buildClient(), existsURL);
        Assert.assertTrue(what.exists());
        Assert.assertNotNull(what.getLasteCacheResponseStatus());
        Assert.assertTrue(ResourceTestHelper.compare(what, new ClassPathResource("data/document.xml")));

        Assert.assertEquals(what.getLasteCacheResponseStatus(), CacheResponseStatus.CACHE_HIT);
    }

    private TestHTTPResource getBean(String fileName, File theDir) {
        final GenericApplicationContext parentContext = new GenericApplicationContext();
        parentContext.refresh(); //THIS IS REQUIRED
        parentContext.getBeanFactory().registerSingleton("theDir", theDir);
        
        final GenericApplicationContext context = new GenericApplicationContext(parentContext);
        XmlBeanDefinitionReader beanDefinitionReader =
                new XmlBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(fileName);
        context.refresh(); // Gotta have this line or the property replacement won't work.
        
        Collection<TestHTTPResource> beans = context.getBeansOfType(TestHTTPResource.class).values();
        Assert.assertEquals(beans.size(), 1);

        return beans.iterator().next();
    }

    @Test public void springLoadMemCache() throws IOException {
        
        final TestHTTPResource what = getBean("classpath:data/MemBackedHTTPBean.xml", null);
        
        Assert.assertTrue(what.exists());
        Assert.assertNotNull(what.getLasteCacheResponseStatus());
        Assert.assertTrue(ResourceTestHelper.compare(what, new ClassPathResource("data/document.xml")));

        Assert.assertEquals(what.getLasteCacheResponseStatus(), CacheResponseStatus.CACHE_HIT);
    }
    
    private void emptyDir(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                emptyDir(f);
            }
            f.delete();
        }
        dir.delete();
    }

    @Test public void springLoadFileCache() throws IOException  {
        File theDir = null;
        Path p = Files.createTempDirectory("HTTPResourceTest");
        try {
            theDir = p.toFile();
    
            final TestHTTPResource what = getBean("classpath:data/FileBackedHTTPBean.xml", theDir);
            
            Assert.assertTrue(what.exists());
            Assert.assertNotNull(what.getLasteCacheResponseStatus());
            Assert.assertTrue(ResourceTestHelper.compare(what, new ClassPathResource("data/document.xml")));

            Assert.assertEquals(what.getLasteCacheResponseStatus(), CacheResponseStatus.CACHE_HIT);
        } finally {
            if (null != theDir) {
                emptyDir(theDir);
            }
        }
    }
}
