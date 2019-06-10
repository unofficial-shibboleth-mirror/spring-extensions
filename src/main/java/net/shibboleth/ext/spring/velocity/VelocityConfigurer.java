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

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.web.context.ServletContextAware;

/**
 * JavaBean to configure Velocity for web usage, via the "configLocation"
 * and/or "velocityProperties" and/or "resourceLoaderPath" bean properties.
 * The simplest way to use this class is to specify just a "resourceLoaderPath";
 * you do not need any further configuration then.
 *
 * <pre class="code">
 * &lt;bean id="velocityConfig" class="org.springframework.web.servlet.view.velocity.VelocityConfigurer"&gt;
 *   &lt;property name="resourceLoaderPath"&gt;&lt;value&gt;/WEB-INF/velocity/&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * This bean must be included in the application context of any application
 * using Spring's {@link VelocityView} for web MVC. It exists purely to configure
 * Velocity; it is not meant to be referenced by application components (just
 * internally by VelocityView). This class implements {@link VelocityConfig}
 * in order to be found by VelocityView without depending on the bean name of
 * this configurer. Each DispatcherServlet may define its own VelocityConfigurer
 * if desired, potentially with different template loader paths.
 *
 * <p>Note that you can also refer to a pre-configured VelocityEngine
 * instance via the "velocityEngine" property, e.g. set up by
 * {@link org.springframework.ui.velocity.VelocityEngineFactoryBean},
 * This allows to share a VelocityEngine for web and email usage, for example.
 *
 * <p>This configurer registers the "spring.vm" Velocimacro library for web views
 * (contained in this package and thus in {@code spring.jar}), which makes
 * all of Spring's default Velocity macros available to the views.
 * This allows for using the Spring-provided macros such as follows:
 *
 * <pre class="code">
 * #springBind("person.age")
 * age is ${status.value}</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Darren Davison
 * 
 * @since 6.0.0
 */
public class VelocityConfigurer extends VelocityEngineFactory
        implements VelocityConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {

    /** Name of the resource loader for Spring's bind macros. */
    private static final String SPRING_MACRO_RESOURCE_LOADER_NAME = "springMacro";

    /** Key for the class of Spring's bind macro resource loader. */
    private static final String SPRING_MACRO_RESOURCE_LOADER_CLASS = "resource.loader.springMacro.class";

    /** Name of Spring's default bind macro library. */
    private static final String SPRING_MACRO_LIBRARY = "net/shibboleth/ext/spring/velocity/spring.vm";

    /** Servlet context. */
    @Nullable private ServletContext servletContext;

    /** Velocity engine. */
    @Nullable private VelocityEngine velocityEngine;
    
    /** {@inheritDoc} */
    public void setServletContext(final ServletContext context) {
        servletContext = context;
    }
    
    /**
     * Initialize VelocityEngineFactory's VelocityEngine
     * if not overridden by a pre-configured VelocityEngine.
     * @see #createVelocityEngine
     * @see #setVelocityEngine
     */
    @Override
    public void afterPropertiesSet() throws IOException, VelocityException {
        if (this.velocityEngine == null) {
            this.velocityEngine = createVelocityEngine();
        }
    }
    
    /**
     * Provides a ClasspathResourceLoader in addition to any default or user-defined
     * loader in order to load the spring Velocity macros from the class path.
     * 
     * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
     */
    @Override
    protected void postProcessVelocityEngine(@Nonnull final VelocityEngine engine) {
        velocityEngine = engine;
        
        velocityEngine.setApplicationAttribute(ServletContext.class.getName(), servletContext);
        velocityEngine.setProperty(
                SPRING_MACRO_RESOURCE_LOADER_CLASS, ClasspathResourceLoader.class.getName());
        velocityEngine.addProperty(
                VelocityEngine.RESOURCE_LOADERS, SPRING_MACRO_RESOURCE_LOADER_NAME);
        velocityEngine.addProperty(
                VelocityEngine.VM_LIBRARY, SPRING_MACRO_LIBRARY);
    }

    /** {@inheritDoc} */
    public VelocityEngine getVelocityEngine() {
        return velocityEngine;
    }

}