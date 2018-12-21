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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.httpclient.HttpClientContextHandler;
import net.shibboleth.utilities.java.support.httpclient.InMemoryCachingHttpClientBuilder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.joda.time.DateTime;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;

/**
 * Test for HTTPResource.
 */
public class HTTPResourceTest {

    private final String existsURL =
            "https://git.shibboleth.net/view/?p=spring-extensions.git;a=blob_plain;f=src/test/resources/data/document.xml;h=e8ec7c0d20c7a6b8193e1868398cda0c28df45ed;hb=HEAD";

    private final String nonExistsURL =
            "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/documxent.xml?view=co";

    private HttpClient client;

    @BeforeClass public void setupClient() throws Exception {
        client = (new HttpClientBuilder()).buildClient();
    }

    @Test public void existsTest() throws IOException {
        final HTTPResource existsResource = new HTTPResource(client, existsURL);
        final HTTPResource notExistsResource = new HTTPResource(client, nonExistsURL);

        Assert.assertTrue(existsResource.exists());
        Assert.assertFalse(notExistsResource.exists());
    }
    
    @Test public void contextHandlerNoopTest() throws IOException {
        final HTTPResource existsResource = new HTTPResource(client, existsURL);
        existsResource.setHttpClientContextHandler(new HttpClientContextHandler() {
            public void invokeBefore(HttpClientContext context, HttpUriRequest request) throws IOException {
            }
            public void invokeAfter(HttpClientContext context, HttpUriRequest request) throws IOException {
            }
        });

        Assert.assertTrue(existsResource.exists());
    }
    
    @Test public void contextHandlerFailBeforeTest() throws IOException {
        final HTTPResource existsResource = new HTTPResource(client, existsURL);
        existsResource.setHttpClientContextHandler(new HttpClientContextHandler() {
            public void invokeBefore(HttpClientContext context, HttpUriRequest request) throws IOException {
                throw new IOException("Fail");
            }
            public void invokeAfter(HttpClientContext context, HttpUriRequest request) throws IOException {
            }
        });

        Assert.assertFalse(existsResource.exists());
    }
    
    @Test public void contextHandlerFailAfterTest() throws IOException {
        final HTTPResource existsResource = new HTTPResource(client, existsURL);
        existsResource.setHttpClientContextHandler(new HttpClientContextHandler() {
            public void invokeBefore(HttpClientContext context, HttpUriRequest request) throws IOException {
            }
            public void invokeAfter(HttpClientContext context, HttpUriRequest request) throws IOException {
                throw new IOException("Fail");
            }
        });

        Assert.assertFalse(existsResource.exists());
    }

    @Test public void testCompare() throws IOException {

        Assert.assertTrue(ResourceTestHelper.compare(new HTTPResource(client, existsURL), new ClassPathResource(
                "net/shibboleth/ext/spring/resource/document.xml")));
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

        final TestHTTPResource what = new TestHTTPResource(client, existsURL);
        Assert.assertTrue(what.exists());
        Assert.assertNull(what.getLastCacheResponseStatus());
        Assert.assertTrue(ResourceTestHelper.compare(what, new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
        Assert.assertNull(what.getLastCacheResponseStatus());
    }

    @Test public void testCachedCache() throws Exception {

        final InMemoryCachingHttpClientBuilder builder = new InMemoryCachingHttpClientBuilder();
        builder.setMaxCacheEntries(3);
        final TestHTTPResource what = new TestHTTPResource(builder.buildClient(), existsURL);
        Assert.assertTrue(what.exists());
        Assert.assertNotNull(what.getLastCacheResponseStatus());
        Assert.assertTrue(ResourceTestHelper.compare(what, new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));

        Assert.assertEquals(what.getLastCacheResponseStatus(), CacheResponseStatus.CACHE_HIT);
    }

    private GenericApplicationContext getContext(final String fileName, final File theDir) {
        final GenericApplicationContext parentContext = new GenericApplicationContext();
        parentContext.refresh(); // THIS IS REQUIRED
        parentContext.getBeanFactory().registerSingleton("theDir", theDir);

        final GenericApplicationContext context = new GenericApplicationContext(parentContext);
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(fileName);
        context.refresh();
        return context;
    }

    @Test public void springLoadMemCache() throws IOException {

        final GenericApplicationContext context = getContext("classpath:net/shibboleth/ext/spring/resource/MemBackedHTTPBean.xml", null);
        try {

            final Collection<TestHTTPResource> beans = context.getBeansOfType(TestHTTPResource.class).values();
            Assert.assertEquals(beans.size(), 1);

            final TestHTTPResource what = beans.iterator().next();

            Assert.assertTrue(what.exists());
            Assert.assertNotNull(what.getLastCacheResponseStatus());
            Assert.assertTrue(ResourceTestHelper.compare(what, new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));

            Assert.assertEquals(what.getLastCacheResponseStatus(), CacheResponseStatus.CACHE_HIT);
        } finally {
            ((GenericApplicationContext) context.getParent()).close();
            context.close();
        }
    }

    private void emptyDir(final File dir) {
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                emptyDir(f);
            }
            Assert.assertTrue(f.delete());
        }
        Assert.assertTrue(dir.delete());
    }

    @Test(enabled=false) public void springLoadFileCache() throws IOException {
        File theDir = null;
        GenericApplicationContext context = null;
        try {
            final Set<PosixFilePermission> prot = Sets.newHashSet(PosixFilePermission.values());
            final Path p = Files.createTempDirectory("HTTPResourceTest", PosixFilePermissions.asFileAttribute(prot));
            theDir = p.toFile();
            context = getContext("classpath:net/shibboleth/ext/spring/resource/MemBackedHTTPBean.xml", null);
            final Collection<TestHTTPResource> beans = context.getBeansOfType(TestHTTPResource.class).values();
            Assert.assertEquals(beans.size(), 1);

            final TestHTTPResource what = beans.iterator().next();

            Assert.assertTrue(what.exists());
            Assert.assertNotNull(what.getLastCacheResponseStatus());
            Assert.assertTrue(ResourceTestHelper.compare(what, new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));

            Assert.assertEquals(what.getLastCacheResponseStatus(), CacheResponseStatus.CACHE_HIT);
        } finally {
            if (null != theDir) {
                emptyDir(theDir);
            }
            if (null != context) {
                ((GenericApplicationContext) context.getParent()).close();
                context.close();
            }
        }
    }

    @Test(timeOut = 2000) public void testCloseResponse() {
        // See IDP-969. This test will timeout if the response is not closed.
        try (final PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager()) {
            final HTTPResource notExistsResource = new HTTPResource(client, nonExistsURL);
            int count = 0;
            while (count <= connMgr.getDefaultMaxPerRoute()) {
                count++;
                try {
                    notExistsResource.getInputStream();
                } catch (final IOException e) {
                    // expected because resource does not exist
                }
            }
        } catch (final IOException e) {
            Assert.fail("Bad URL", e);
        }
    }
}
