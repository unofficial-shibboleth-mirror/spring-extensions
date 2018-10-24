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

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.NestedIOException;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.springframework.web.util.NestedServletException;

/**
 * View using the Velocity template engine.
 *
 * <p>Exposes the following JavaBean properties:
 * <ul>
 * <li><b>url</b>: the location of the Velocity template to be wrapped,
 * relative to the Velocity resource loader path (see VelocityConfigurer).
 * <li><b>encoding</b> (optional, default is determined by Velocity configuration):
 * the encoding of the Velocity template file
 * <li><b>velocityFormatterAttribute</b> (optional, default=null): the name of
 * the VelocityFormatter helper object to expose in the Velocity context of this
 * view, or {@code null} if not needed. VelocityFormatter is part of standard Velocity.
 * <li><b>cacheTemplate</b> (optional, default=false): whether or not the Velocity
 * template should be cached. It should normally be true in production, but setting
 * this to false enables us to modify Velocity templates without restarting the
 * application (similar to JSPs). Note that this is a minor optimization only,
 * as Velocity itself caches templates in a modification-aware fashion.
 * </ul>
 *
 * <p>Note: Spring 3.0's VelocityView requires Velocity 1.4 or higher.</p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Dave Syer
 * 
 * @since 6.0.0
 */
public class VelocityView extends AbstractTemplateView {

    /** Template encoding. */
    @Nullable private String encoding;

    /** Caching flag. */
    private boolean cacheTemplate;

    /** Velocity engine. */
    @Nullable private VelocityEngine velocityEngine;

    /** The template. */
    @Nullable private Template template;

    /**
     * Set the encoding of the Velocity template file.
     * 
     * <p>Default is determined
     * by the VelocityEngine: "ISO-8859-1" if not specified otherwise.
     * Specify the encoding in the VelocityEngine rather than per template
     * if all your templates share a common encoding.</p>
     * 
     * @param enc encoding
     */
    public void setEncoding(@Nullable final String enc) {
        encoding = enc;
    }

    /**
     * Return the encoding for the Velocity template.
     * 
     * @return encoding
     */
    @Nullable protected String getEncoding() {
        return encoding;
    }

    /**
     * Set whether the Velocity template should be cached (default is "false").
     * 
     * <p>It should normally be true in production, but setting this to false enables us to
     * modify Velocity templates without restarting the application (similar to JSPs).</p>
     * 
     * <p>Note that this is a minor optimization only, as Velocity itself caches
     * templates in a modification-aware fashion.</p>
     * 
     * @param flag flag to set
     */
    public void setCacheTemplate(final boolean flag) {
        cacheTemplate = flag;
    }

    /**
     * Return whether the Velocity template should be cached.
     * 
     * @return whether template should be cached
     */
    protected boolean isCacheTemplate() {
        return cacheTemplate;
    }

    /**
     * Set the VelocityEngine to be used by this view.
     * 
     * <p>If this is not set, the default lookup will occur: A single {@link VelocityConfig}
     * is expected in the current web application context, with any bean name.</p>
     * 
     * @param engine velocity engine
     */
    public void setVelocityEngine(@Nullable final VelocityEngine engine) {
        velocityEngine = engine;
    }

    /**
     * Return the VelocityEngine used by this view.
     * 
     * @return engine
     */
    @Nullable protected VelocityEngine getVelocityEngine() {
        return velocityEngine;
    }

    /**
     * Invoked on startup. Looks for a single {@link VelocityConfig} bean to
     * find the relevant VelocityEngine for this factory.
     */
    @Override
    protected void initApplicationContext() throws BeansException {
        super.initApplicationContext();

        if (getVelocityEngine() == null) {
            // No explicit VelocityEngine: try to autodetect one.
            setVelocityEngine(autodetectVelocityEngine());
        }
    }

    /**
     * Autodetect a VelocityEngine via the ApplicationContext.
     * 
     * <p>Called if no explicit VelocityEngine has been specified.</p>
     * 
     * @return the VelocityEngine to use
     * 
     * @throws BeansException if no VelocityEngine could be found
     */
    @Nonnull protected VelocityEngine autodetectVelocityEngine() throws BeansException {
        try {
            final VelocityConfig velocityConfig = BeanFactoryUtils.beanOfTypeIncludingAncestors(
                    getApplicationContext(), VelocityConfig.class, true, false);
            return velocityConfig.getVelocityEngine();
        } catch (final NoSuchBeanDefinitionException ex) {
            throw new ApplicationContextException(
                    "Must define a single VelocityConfig bean in this web application context " +
                    "(may be inherited): VelocityConfigurer is the usual implementation. " +
                    "This bean may be given any name.", ex);
        }
    }
    
    /**
     * Check that the Velocity template used for this view exists and is valid.
     * <p>Can be overridden to customize the behavior, for example in case of
     * multiple templates to be rendered into a single view.
     */
    @Override
    public boolean checkResource(@Nullable final Locale locale) throws Exception {
        try {
            // Check that we can get the template, even if we might subsequently get it again.
            template = getTemplate(getUrl());
            return true;
        } catch (final ResourceNotFoundException ex) {
            /*
             * TODO: uncomment once we have a commons logging answer
            if (logger.isDebugEnabled()) {
                logger.debug("No Velocity view found for URL: " + getUrl());
            }
            */
            return false;
        } catch (final Exception ex) {
            throw new NestedIOException(
                    "Could not load Velocity template for URL [" + getUrl() + "]", ex);
        }
    }


    /**
     * Process the model map by merging it with the Velocity template.
     * 
     * <p>Output is directed to the servlet response.
     * This method can be overridden if custom behavior is needed.</p>
     */
    @Override
    protected void renderMergedTemplateModel(final Map<String,Object> model,
            final HttpServletRequest request, final HttpServletResponse response) throws Exception {

        final Context velocityContext = createVelocityContext(model, request, response);

        doRender(velocityContext, response);
    }

    /**
     * Create a Velocity Context instance for the given model,
     * to be passed to the template for merging.
     * 
     * <p>The default implementation delegates to {@link #createVelocityContext(Map)}.
     * Can be overridden for a special context class, for example ChainedContext which
     * is part of the view package of Velocity Tools. ChainedContext is needed for
     * initialization of ViewTool instances.</p>
     *
     * @param model the model Map, containing the model attributes to be exposed to the view
     * @param request current HTTP request
     * @param response current HTTP response
     * 
     * @return the Velocity Context
     * 
     * @throws Exception if there's a fatal error while creating the context
     */
    protected Context createVelocityContext(@Nullable final Map<String,Object> model,
            @Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response) throws Exception {

        return createVelocityContext(model);
    }

    /**
     * Create a Velocity Context instance for the given model,
     * to be passed to the template for merging.
     * 
     * <p>Default implementation creates an instance of Velocity's
     * VelocityContext implementation class.</p>
     * 
     * @param model the model Map, containing the model attributes
     * to be exposed to the view
     * 
     * @return the Velocity Context
     * 
     * @throws Exception if there's a fatal error while creating the context
     */
    protected Context createVelocityContext(final Map<String, Object> model) throws Exception {
        return new VelocityContext(model);
    }

    /**
     * Render the Velocity view to the given response, using the given Velocity
     * context which contains the complete template model to use.
     * 
     * <p>The default implementation renders the template specified by the "url"
     * bean property, retrieved via {@code getTemplate}. It delegates to the
     * {@code mergeTemplate} method to merge the template instance with the
     * given Velocity context.</p>
     * 
     * <p>Can be overridden to customize the behavior, for example to render
     * multiple templates into a single view.</p>
     * 
     * @param context the Velocity context to use for rendering
     * @param response servlet response (use this to get the OutputStream or Writer)
     * 
     * @throws Exception if thrown by Velocity
     */
    protected void doRender(final Context context, final HttpServletResponse response)
            throws Exception {
        /*
         * TODO: uncomment once we have a commons logging solution
        if (logger.isDebugEnabled()) {
            logger.debug("Rendering Velocity template [" + getUrl() + "] in VelocityView '" + getBeanName() + "'");
        }
        */
        mergeTemplate(getTemplate(), context, response);
    }

    /**
     * Retrieve the Velocity template to be rendered by this view.
     * 
     * <p>By default, the template specified by the "url" bean property will be
     * retrieved: either returning a cached template instance or loading a fresh
     * instance (according to the "cacheTemplate" bean property)</p>
     * 
     * @return the Velocity template to render
     * 
     * @throws Exception if thrown by Velocity
     */
    protected Template getTemplate() throws Exception {
        // We already hold a reference to the template, but we might want to load it
        // if not caching. Velocity itself caches templates, so our ability to
        // cache templates in this class is a minor optimization only.
        if (isCacheTemplate() && template != null) {
            return template;
        } else {
            return getTemplate(getUrl());
        }
    }

    /**
     * Retrieve the Velocity template specified by the given name,
     * using the encoding specified by the "encoding" bean property.
     * 
     * <p>Can be called by subclasses to retrieve a specific template,
     * for example to render multiple templates into a single view.</p>
     * 
     * @param name the file name of the desired template
     * 
     * @return the Velocity template
     * 
     * @throws Exception if thrown by Velocity
     */
    protected Template getTemplate(final String name) throws Exception {
        return getEncoding() != null ?
                getVelocityEngine().getTemplate(name, getEncoding()) :
                getVelocityEngine().getTemplate(name);
    }

    /**
     * Merge the template with the context.
     * 
     * @param t the template to merge
     * @param context the Velocity context to use for rendering
     * @param response servlet response (use this to get the OutputStream or Writer)
     * 
     * @throws Exception if thrown by Velocity
     */
    protected void mergeTemplate(final Template t, final Context context,
            @Nonnull final HttpServletResponse response) throws Exception {

        try {
            t.merge(context, response.getWriter());
        } catch (final MethodInvocationException ex) {
            final Throwable cause = ex.getWrappedThrowable();
            throw new NestedServletException(
                    "Method invocation failed during rendering of Velocity view with name '" +
                    getBeanName() + "': " + ex.getMessage() + "; reference [" + ex.getReferenceName() +
                    "], method '" + ex.getMethodName() + "'",
                    cause==null ? ex : cause);
        }
    }

}