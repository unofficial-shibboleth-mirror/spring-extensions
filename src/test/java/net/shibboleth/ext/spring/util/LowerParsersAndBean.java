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

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

public class LowerParsersAndBean extends BaseSpringNamespaceHandler {
    
    protected static final String NAMESPACE = "urn:mace:shibboleth:2.0:nested";
    
    private String message;
    
    public void setMessage(String theMessage) {
        message = theMessage;
    }
    
    public String getMessage() {
        return message;
    }

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(new QName(NAMESPACE, "LowerElement"), new LowerElementParser());
        registerBeanDefinitionParser(new QName(NAMESPACE, "OuterElement"), new OuterElementParser());
        final String adjustedName=NAMESPACE.replaceAll("\\:", "-");  
        initializeOtherHandlers(adjustedName);
    }

    static class LowerElementParser extends AbstractCustomBeanDefinitionParser {
        
        /** {@inheritDoc} */
        protected Class<LowerParsersAndBean> getBeanClass(Element element) {
            return LowerParsersAndBean.class;
        }
        
        /** {@inheritDoc} */
        @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
                @Nonnull final BeanDefinitionBuilder builder) {
            builder.addPropertyValue("message", 
                    AttributeSupport.getAttributeValue(config, null, "theMessage"));
        }
    }
    
    static class OuterElementParser implements BeanDefinitionParser {
        
        /** {@inheritDoc} */
        public BeanDefinition parse(final Element config, final ParserContext parserContext) {
            SpringSupport.parseCustomElements(ElementSupport.getChildElements(config), parserContext);
            return null;
        }

    }
    
}
