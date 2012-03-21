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

import net.shibboleth.utilities.java.support.resource.ClasspathResource;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** {@link DomDocumentFactoryBean} unit tests. */
public class DomDocumentFactoryBeanTest {

    /**
     * Test normal use of the factory.
     * 
     * @throws Exception if anything at all goes wrong
     */
    @Test
    public void getObject() throws Exception {
        DomDocumentFactoryBean factory = new DomDocumentFactoryBean();
        Resource resource = new ClasspathResource("data/document.xml");
        resource.initialize();
        BasicParserPool pool = new BasicParserPool();
        pool.initialize();
        factory.setDocumentResource(resource);
        factory.setParserPool(pool);
        Document doc = factory.getObject();

        // look at the result
        Element element = doc.getDocumentElement();
        Assert.assertEquals(element.getLocalName(), "docElement");

        // check singleton behaviour
        Assert.assertEquals(factory.isSingleton(), true, "singleton assertion");
        Document doc2 = factory.getObject();
        Assert.assertEquals(doc2, doc, "singleton equality");
    }

    /**
     * Test that the claimed object type of the factory is as expected.
     */
    @Test
    public void getObjectType() {
        DomDocumentFactoryBean factory = new DomDocumentFactoryBean();
        Assert.assertEquals(factory.getObjectType(), Document.class, "object type");
    }

}
