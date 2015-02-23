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

import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

import org.apache.http.client.HttpClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for FileBackedHTTPResource.
 */
public class FileBackedHTTPResourceTest {

    private final String existsURL = "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/document.xml?view=co";
    private final String nonExistsURL = "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/documxent.xml?view=co";
    private String existsFile;
   
    private HttpClient client;

    @BeforeClass public void setupClient() throws Exception {
        client = (new HttpClientBuilder()).buildClient();
        File file = File.createTempFile("FileBackedHTTPResourceTest1", ".xml");
        existsFile = file.getAbsolutePath();
    }
    
    @AfterClass public void deleteFile() {
        File f = new File(existsFile);
        if (f.exists()) {
            f.delete();
        }
    }
    
    @Test public void existsTest() throws IOException {
       final Resource existsResource = new FileBackedHTTPResource(client, existsURL, new FileSystemResource(existsFile));
       final Resource notExistsResource = new FileBackedHTTPResource(client, nonExistsURL,  new FileSystemResource(existsFile+"ZZZ"));
        
        Assert.assertTrue(existsResource.exists());
        Assert.assertFalse(notExistsResource.exists());
    }
    
    @Test public void testCompare() throws IOException {
        
        Assert.assertTrue(ResourceTestHelper.compare(new FileBackedHTTPResource(client, existsURL, new FileSystemResource(existsFile)), new ClassPathResource("data/document.xml")));
        // With that done compare via the backup
        Assert.assertTrue(ResourceTestHelper.compare(new FileBackedHTTPResource(client, nonExistsURL, new FileSystemResource(existsFile)), new ClassPathResource("data/document.xml")));
    }
}
