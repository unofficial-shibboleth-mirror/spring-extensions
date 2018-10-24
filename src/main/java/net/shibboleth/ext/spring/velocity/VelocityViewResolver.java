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

package net.shibboleth.ext.spring.velocity;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

/**
 * Convenience subclass of {@link org.springframework.web.servlet.view.UrlBasedViewResolver}
 * that supports {@link VelocityView} (i.e. Velocity templates) and custom subclasses of it.
 *
 * <p><b>Note:</b> When chaining ViewResolvers, a VelocityViewResolver will
 * check for the existence of the specified template resources and only return
 * a non-null View object if the template was actually found.
 *
 * @author Juergen Hoeller
 * 
 * @since 6.0.0
 */
public class VelocityViewResolver extends AbstractTemplateViewResolver {
    
    /** Constructor. */
    public VelocityViewResolver() {
        setViewClass(VelocityView.class);
    }
    
}