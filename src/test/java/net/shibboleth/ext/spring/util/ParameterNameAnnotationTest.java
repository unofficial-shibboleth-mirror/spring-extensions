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

package net.shibboleth.ext.spring.util;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class ParameterNameAnnotationTest {

    @Test public void testNoAnnotationFilter() {
        
        final GenericApplicationContext context = new GenericApplicationContext();
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/ext/spring/util/paramBeans.xml");
        context.refresh();
        
        final ParamClass byNumber = context.getBean("TheBeanRemainsTheSame", ParamClass.class);
        Assert.assertEquals(byNumber.getP1(), "Param the First");
        Assert.assertEquals(byNumber.getP2(), "Param the Second");
        
        final ParamClass byId = context.getBean("InThroughTheOutBean", ParamClass.class);

        //
        // Swapped
        //
        Assert.assertEquals(byId.getP2(), "Param the First");
        Assert.assertEquals(byId.getP1(), "Param the Second");

    }
    
    @Test(enabled=false) public void testWithAnnotationFilter() {
        
        final GenericApplicationContext context = new GenericApplicationContext();
        final XmlBeanDefinitionReader beanDefinitionReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/ext/spring/util/paramBeans.xml");
        context.refresh();
        
        final ParamClass byNumber = context.getBean("TheBeanRemainsTheSame", ParamClass.class);
        Assert.assertEquals(byNumber.getP1(), "Param the First");
        Assert.assertEquals(byNumber.getP2(), "Param the Second");
        
        final ParamClass byId = context.getBean("InThroughTheOutBean", ParamClass.class);

        //
        // Swapped
        //
        Assert.assertEquals(byId.getP1(), "Param the First");
        Assert.assertEquals(byId.getP2(), "Param the Second");

    }

}
