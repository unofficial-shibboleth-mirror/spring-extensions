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
import java.nio.file.Path;
import java.util.Collection;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;

/**
 *
 */
public class Idp1326{
    
    private String parentAsString;
    private String separator;
    private Resource resource;
    
    @BeforeClass public void getFileSystemDetails() throws IOException {
        resource = new ClassPathResource("net/shibboleth/ext/spring/resource/idp1326.xml");
        final Path path = resource.getFile().toPath();
        
        separator = path.getFileSystem().getSeparator();
        Assert.assertEquals(separator.length(), 1);
        parentAsString = path.getParent().toAbsolutePath().toString();
    }
    

    /*
     * Fails on Windows
     */
    @Test(enabled=false) public void testPropFileNative() {
        testPropFile(parentAsString);
    }
    
    @Test public void testPropFileJava() {
        final StringBuilder sb = new StringBuilder(parentAsString);
        if (!"/".equals(separator)) {
            int i = sb.indexOf(separator);
            while (i > 0) {
                sb.replace(i, i+1, "/");
                i = sb.indexOf(separator);
            }
        }
        testPropFile(sb.toString());
    }

    
    private void testPropFile(String idpHome) {
        
        GenericApplicationContext context = new FilesystemGenericApplicationContext();
        
        final MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        final MockPropertySource mockEnvVars = new MockPropertySource();
        mockEnvVars.setProperty("idp.home", idpHome);
        
        propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockEnvVars);
        
        final PropertySourcesPlaceholderConfigurer placeholderConfig = new PropertySourcesPlaceholderConfigurer();
        placeholderConfig.setPlaceholderPrefix("%{");
        placeholderConfig.setPlaceholderSuffix("}");
        placeholderConfig.setPropertySources(propertySources);
        
        context.addBeanFactoryPostProcessor(placeholderConfig);

        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);
        beanDefinitionReader.loadBeanDefinitions(resource);
        
        context.refresh();
        final Collection<String> beans = context.getBeansOfType(String.class).values();
        Assert.assertEquals(beans.size(), 1);

        Assert.assertEquals(beans.iterator().next(), idpHome);
    }
}
