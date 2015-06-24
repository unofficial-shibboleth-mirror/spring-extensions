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

package net.shibboleth.ext.spring.factory;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.net.URISupport;

import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link QueryPassthroughFactoryBean} unit tests. */
public class QueryPassthroughFactoryBeanTest {

    private String basePath;
    private MockHttpServletRequest request;
    private QueryPassthroughFactoryBean factory;
    
    @BeforeMethod
    public void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/original/path");
        request.addParameter("ignored", "value");
        
        basePath = "/test/path";
        
        factory = new QueryPassthroughFactoryBean();
        factory.setHttpServletRequest(request);
        factory.setBasePath(basePath);
        factory.setParameters(Arrays.asList("foo", "bar"));
    }
    
    @Test
    public void testNoParameters() throws Exception {
        factory.setParameters(Collections.<String>emptyList());
        Assert.assertEquals(factory.getObject(), basePath);
    }

    @Test
    public void testNoParametersSet() throws Exception {
        Assert.assertEquals(factory.getObject(), basePath);
    }
    
    @Test
    public void testSimple() throws Exception {
        request.addParameter("foo", "value");
        
        final String url = factory.getObject();
        
        final URI uri = new URI(url);
        Assert.assertEquals(uri.getPath(), basePath);
        
        final List<Pair<String,String>> params = URISupport.parseQueryString(uri.getRawQuery());
        Assert.assertEquals(params.size(), 1);
        Assert.assertEquals(params.get(0), new Pair("foo", "value"));
    }

    @Test
    public void testMultiple() throws Exception {
        request.addParameter("foo", "value");
        request.addParameter("bar", "value");
        
        final String url = factory.getObject();
        
        final URI uri = new URI(url);
        Assert.assertEquals(uri.getPath(), basePath);
        
        final List<Pair<String,String>> params = URISupport.parseQueryString(uri.getRawQuery());
        Assert.assertEquals(params.size(), 2);
        Assert.assertEquals(params.get(0), new Pair("foo", "value"));
        Assert.assertEquals(params.get(1), new Pair("bar", "value"));
    }

    @Test
    public void testMultiValue() throws Exception {
        request.addParameter("foo", "value");
        request.addParameter("foo", "value&2");
        
        final String url = factory.getObject();
        
        final URI uri = new URI(url);
        Assert.assertEquals(uri.getPath(), basePath);
        
        final List<Pair<String,String>> params = URISupport.parseQueryString(uri.getRawQuery());
        Assert.assertEquals(params.size(), 2);
        Assert.assertEquals(params.get(0), new Pair("foo", "value"));
        Assert.assertEquals(params.get(1), new Pair("foo", "value&2"));
    }

    @Test
    public void testEmptyValue() throws Exception {
        request.addParameter("foo", "value");
        request.addParameter("bar", (String) null);
        
        final String url = factory.getObject();
        
        final URI uri = new URI(url);
        Assert.assertEquals(uri.getPath(), basePath);
        
        final List<Pair<String,String>> params = URISupport.parseQueryString(uri.getRawQuery());
        Assert.assertEquals(params.size(), 2);
        Assert.assertEquals(params.get(0), new Pair("foo", "value"));
        Assert.assertEquals(params.get(1), new Pair("bar", null));
    }
    
    @Test
    public void getObjectType() {
        Assert.assertEquals(factory.getObjectType(), String.class);
    }

}