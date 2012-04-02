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

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resource.Resource;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.w3c.dom.Document;

/** Spring bean factory for producing a {@link Document} from a resource. */
public class DomDocumentFactoryBean implements FactoryBean<Document> {

    /** Resource to load the document from. */
    private Resource documentResource;

    /** Parser pool to use when parsing the document. */
    private BasicParserPool parserPool;

    /** Resulting {@link Document}. */
    private Document document;

    /**
     * Sets the resource containing the document to be parsed.
     *
     * @param resource resource, never null
     */
    public void setDocumentResource(@Nonnull final Resource resource) {
        documentResource = Constraint.isNotNull(resource, "XML Resource can not be null");
    }

    /**
     * Sets the parser pool to be used to parse the file.
     *
     * @param pool parser pool, never null.
     */
    public void setParserPool(@Nonnull final BasicParserPool pool) {
        parserPool = pool;
    }

    /** {@inheritDoc} */
    @Nonnull public synchronized Document getObject() throws Exception {
        if (document == null) {
            if(documentResource == null){
                throw new BeanCreationException("Document resource must be provided in order to use this factory.");
            }
            
            if(parserPool == null){
                throw new BeanCreationException("Parser pool must be provided in order to use this factory.");
            }
            
            document = parserPool.parse(documentResource.getInputStream());
        }
        
        return document;
    }

    /** {@inheritDoc} */
    @Nonnull public Class<?> getObjectType() {
        return Document.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return true;
    }
}