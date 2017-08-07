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
import java.text.ParseException;
import java.util.Collection;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;

/**
 * test,
 */
public class SVNRunnableTest {

    static private final String ORIGINAL_TEXT = "This is a Test Resource which will be superseded by other Data";

    static private final int ORIGINAL_VERSION = 1;

    static private final String PATH = "net/shibboleth/ext/spring/resource/";

    static private final String FILENAME = "TestResource.txt";

    static private final String CLASSPATH_PATH = PATH + FILENAME;

    private SVNClientManager clientManager;

    private Resource comparer = new ClassPathResource(CLASSPATH_PATH);

    private SVNURL url;

    private File theDir;
    
    @BeforeClass public void setup() throws SVNException, IOException {
        final ISVNAuthenticationManager authnManager = new BasicAuthenticationManager(null);
        clientManager = SVNClientManager.newInstance();
        clientManager.setAuthenticationManager(authnManager);
        
        final String theDirPath = new ClassPathResource(PATH).getFile().getAbsolutePath();

        url = SVNURL.create("file", null, "", -1, theDirPath + "/SVN", false);
    }

    @BeforeMethod public void makeDir() throws IOException {
        final Path p = Files.createTempDirectory("SVNRunnableTest");
        theDir = p.toFile();
    }

    private void emptyDir(final File dir) {
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                emptyDir(f);
            }
            f.delete();
        }
    }

    @AfterMethod public void emptyDir() {
        emptyDir(theDir);
        theDir.delete();
        theDir = null;
    }
    
    private Resource makeResource(
            final SVNClientManager svnClientMgr,
            final SVNURL repositoryUrl,
            final File workingCopy,
            final long workingRevision,
            final String resourceFile)
    {
        final SVNRunnable runnable = new SVNRunnable(svnClientMgr, repositoryUrl, workingCopy, workingRevision);
        
        return new RunnableFileSystemResource(new File(workingCopy, resourceFile), runnable);
    }
    

    @Test(enabled=true) public void testRevision() throws IOException, ParseException {
        final Resource resource = makeResource(clientManager, url, theDir, ORIGINAL_VERSION, FILENAME);
        Assert.assertTrue(resource.exists());

        final Resource other = new ByteArrayResource(ORIGINAL_TEXT.getBytes());

        Assert.assertTrue(ResourceTestHelper.compare(other, resource));
        // Check rewind and check the compare code
        Assert.assertFalse(ResourceTestHelper.compare(comparer, resource));
        Assert.assertTrue(ResourceTestHelper.compare(other, resource));
    }

    @Test(enabled=true) public void testNotExist() {
        final Resource resource = makeResource(clientManager, url, theDir, 0, FILENAME);
        Assert.assertFalse(resource.exists());

    }

    @Test(enabled=true) public void testMain() throws IOException {
        final Resource resource = makeResource(clientManager, url, theDir, -1, FILENAME);
        Assert.assertTrue(resource.exists());

        Assert.assertTrue(ResourceTestHelper.compare(comparer, resource));

    }

    private GenericApplicationContext getContext(final String fileName) {
        final GenericApplicationContext parentContext = new GenericApplicationContext();
        parentContext.refresh(); // THIS IS REQUIRED
        parentContext.getBeanFactory().registerSingleton("theDir", theDir);

        final GenericApplicationContext context = new GenericApplicationContext(parentContext);
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(fileName);

        final MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("the.SVN.Dir", url.getPath());
        final MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);

        final PropertySourcesPlaceholderConfigurer placeholderConfig = new PropertySourcesPlaceholderConfigurer();
        placeholderConfig.setPlaceholderPrefix("%{");
        placeholderConfig.setPlaceholderSuffix("}");
        placeholderConfig.setPropertySources(propertySources);

        context.addBeanFactoryPostProcessor(placeholderConfig);

        context.refresh(); 
        return context;

    }

    @Test(enabled=true) public void testSpringLoad() {

        final GenericApplicationContext context = getContext("classpath:"+PATH+"SVNBean.xml");

        
        try {
            final Collection<Resource> beans = context.getBeansOfType(Resource.class).values();
            Assert.assertEquals(beans.size(), 1);

            final Resource r = beans.iterator().next();

            Assert.assertTrue(r.exists());
        } finally {
            ((GenericApplicationContext)context.getParent()).close();
            context.close();
        }
    }
}
