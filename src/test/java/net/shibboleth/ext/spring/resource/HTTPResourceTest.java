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

import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

import org.apache.http.client.HttpClient;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test for HTTPResource.
 */
public class HTTPResourceTest {

    private final String existsURL = "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/document.xml?view=co";
    private final String nonExistsURL = "http://svn.shibboleth.net/view/utilities/spring-extensions/trunk/src/test/resources/data/documxent.xml?view=co";
    
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
        
        Assert.assertTrue(ResourceTestHelper.compare(new HTTPResource(client, existsURL), new ClassPathResource("data/document.xml")));
    }
    
    @Test public void testRelated() throws IOException {

        // Chose a file unlikely to change.  Do not use the svn thing because the date will not be there

        final HTTPResource parent = new HTTPResource(client, "http://shibboleth.net/downloads/identity-provider/2.0.0/");
        final HTTPResource child = parent.createRelative("shibboleth-idp-2.0.0-bin.zip");
        
        final long when = child.lastModified();
        final long size = child.contentLength();
        final String whenAsString = new DateTime(when).toString();
  
        Assert.assertEquals("Expected date of " + whenAsString + " did not match)", 1205848652000L, when);
        Assert.assertEquals("Size mismatch", 20784226L, size);
    }
    
}
