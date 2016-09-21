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

import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.ParserPool;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

/** Spring bean factory for producing a {@link Document} from a resource. */
public class DOMDocumentFactoryBean implements FactoryBean<Document> {

    /** Resource to load the document from. */
    @Nullable private Resource resource;

    /** Parser pool to use when parsing the document. */
    @Nullable private ParserPool parserPool;

    /** Resulting {@link Document}. */
    @Nullable private Document document;

    /**
     * Sets the resource containing the document to be parsed.
     *
     * @param domResource resource, never null
     */
    public void setResource(@Nonnull final Resource domResource) {
        resource = Constraint.isNotNull(domResource, "resource cannot be null");
    }

    /**
     * Sets the parser pool to be used to parse the file.
     *
     * @param pool parser pool, never null.
     */
    public void setParserPool(@Nonnull final ParserPool pool) {
        parserPool = Constraint.isNotNull(pool, "ParserPool cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public synchronized Document getObject() throws Exception {
        if (document == null) {
            if (resource == null){
                throw new BeanCreationException("Document resource must be provided in order to use this factory.");
            }
            
            if (parserPool == null){
                throw new BeanCreationException("ParserPool must be provided in order to use this factory.");
            }
            
            try (InputStream is = resource.getInputStream()) {
                document = parserPool.parse(is);
            }
        }
        
        return document;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public Class<?> getObjectType() {
        return Document.class;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSingleton() {
        return true;
    }
}