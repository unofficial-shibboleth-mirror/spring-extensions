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

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

import org.apache.http.client.HttpClient;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for FileBackedHTTPResource.
 */
public class FileBackedHTTPResourceTest {

    private final String existsURL =
            "https://git.shibboleth.net/view/?p=spring-extensions.git;a=blob_plain;f=src/test/resources/data/document.xml;h=e8ec7c0d20c7a6b8193e1868398cda0c28df45ed;hb=HEAD";

    private final String nonExistsURL =
            "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/documxent.xml?view=co";

    private String existsFile;

    private HttpClient client;

    @BeforeClass public void setupClient() throws Exception {
        client = (new HttpClientBuilder()).buildClient();
        final File file = File.createTempFile("FileBackedHTTPResourceTest1", ".xml");
        existsFile = file.getAbsolutePath();
    }

    @AfterClass public void deleteFile() {
        final File f = new File(existsFile);
        if (f.exists()) {
            f.delete();
        }
    }

    @SuppressWarnings("deprecation") @Test public void existsTest() throws IOException {
        final Resource existsResource = new FileBackedHTTPResource(existsFile, client, existsURL);
        final Resource notExistsResource =
                new FileBackedHTTPResource(client, nonExistsURL, new FileSystemResource(existsFile + "ZZZ"));

        Assert.assertTrue(existsResource.exists());
        Assert.assertFalse(notExistsResource.exists());
    }

    @SuppressWarnings("deprecation") @Test public void testCompare() throws IOException {

        Assert.assertTrue(ResourceTestHelper.compare(new FileBackedHTTPResource(client, existsURL,
                new FileSystemResource(existsFile)), new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
        // With that done compare via the backup
        Assert.assertTrue(ResourceTestHelper.compare(new FileBackedHTTPResource(existsFile, client, nonExistsURL),
                new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
    }

    public GenericApplicationContext getContext(final String location) {

        final MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("file.name", existsFile);
        mockEnvVars.setProperty("the.url", existsURL);

        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
        context.setDisplayName("ApplicationContext");

        final MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);

        final PropertySourcesPlaceholderConfigurer placeholderConfig = new PropertySourcesPlaceholderConfigurer();
        placeholderConfig.setPlaceholderPrefix("%{");
        placeholderConfig.setPlaceholderSuffix("}");
        placeholderConfig.setPropertySources(propertySources);

        context.addBeanFactoryPostProcessor(placeholderConfig);

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions(location);

        beanDefinitionReader.setValidating(true);

        context.refresh();

        return context;

    }

    @Test public void testParsingOld() throws IOException {

        final GenericApplicationContext context = getContext("net/shibboleth/ext/spring/resource/oldStyle.xml");

        try {

            Assert.assertTrue(ResourceTestHelper.compare(context.getBean("namedString", FileBackedHTTPResource.class),
                    new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
            Assert.assertTrue(ResourceTestHelper.compare(context.getBean("namedFileString",
                    FileBackedHTTPResource.class), new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
            Assert.assertTrue(ResourceTestHelper.compare(context.getBean("namedURL", FileBackedHTTPResource.class),
                    new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
            Assert.assertTrue(ResourceTestHelper.compare(context
                    .getBean("numberedString", FileBackedHTTPResource.class),
                    new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
            Assert.assertTrue(ResourceTestHelper.compare(context.getBean("numberedURL", FileBackedHTTPResource.class),
                    new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));

        } finally {
            context.close();
        }
    }

    @Test public void testParsingNew() throws IOException {

        final GenericApplicationContext context = getContext("net/shibboleth/ext/spring/resource/newStyle.xml");
        try {

            Assert.assertTrue(ResourceTestHelper.compare(context.getBean("namedString", FileBackedHTTPResource.class),
                    new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
            Assert.assertTrue(ResourceTestHelper.compare(context.getBean("namedURL", FileBackedHTTPResource.class),
                    new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
            Assert.assertTrue(ResourceTestHelper.compare(context
                    .getBean("numberedString", FileBackedHTTPResource.class),
                    new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
            Assert.assertTrue(ResourceTestHelper.compare(context.getBean("numberedURL", FileBackedHTTPResource.class),
                    new ClassPathResource("net/shibboleth/ext/spring/resource/document.xml")));
        } finally {
            context.close();
        }
    }

}
