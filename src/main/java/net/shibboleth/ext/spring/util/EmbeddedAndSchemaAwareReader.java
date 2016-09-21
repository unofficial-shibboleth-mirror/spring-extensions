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

import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An extension to our {@link SchemaTypeAwareBeanDefinitionDocumentReader} which, in addition
 * allows parsing of embedded &lt;beans&gt; statements (by the native spring parsers).
 */
public class EmbeddedAndSchemaAwareReader extends SchemaTypeAwareBeanDefinitionDocumentReader {

    /** {@inheritDoc}
     * 
     * We call the parent method for the custom schema (which is expected to not do anything special
     * with the &lt;beans&gt; statement) and then we call again to handle the beans statements which
     * we have explicitly pulled out.
     *  */
    @Override public void registerBeanDefinitions(final Document doc, final XmlReaderContext readerContext)
            throws BeanDefinitionStoreException {
        
        super.registerBeanDefinitions(doc, readerContext);
        
        final List<Element> beans = ElementSupport.getChildElements(doc.getDocumentElement(), 
                new QName(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI, NESTED_BEANS_ELEMENT));
        if (beans.isEmpty()) {
            return;
        }
        for (final Element elem : beans) {
            doRegisterBeanDefinitions(elem);
        }
    }
}
