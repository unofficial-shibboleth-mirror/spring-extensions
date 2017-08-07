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

package net.shibboleth.ext.spring.resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.scripting.AbstractScriptEvaluator;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

/**
 * A Runnable which executes a script.
 */
public class ScriptedRunnable extends AbstractIdentifiableInitializableComponent
        implements Runnable, UnmodifiableComponent {

    /** What is run. */
    @NonnullAfterInit private EvaluableScript script;

    /** Evaluator. */
    @NonnullAfterInit private RunnableScriptEvaluator scriptEvaluator;

    /** The log. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedRunnable.class);

    /** Custom object for script. */
    @Nullable private Object customObject;

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == script) {
            throw new ComponentInitializationException("No script has been provided");
        }

        scriptEvaluator = new RunnableScriptEvaluator(script);
        scriptEvaluator.setCustomObject(customObject);

        final StringBuilder builder = new StringBuilder("ScriptedRunnable '").append(getId()).append("':");
        scriptEvaluator.setLogPrefix(builder.toString());
    }

    /**
     * Return the custom (externally provided) object.
     * 
     * @return the custom object
     */
    @Nullable public Object getCustomObject() {
        return customObject;
    }

    /**
     * Set the custom (externally provided) object.
     * 
     * @param object the custom object
     */
    public void setCustomObject(@Nullable final Object object) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        customObject = object;
    }

    /**
     * Gets the script to be evaluated.
     * 
     * @return the script to be evaluated
     */
    @NonnullAfterInit public EvaluableScript getScript() {
        return script;
    }

    /**
     * Sets the script to be evaluated.
     * 
     * @param matcherScript the script to be evaluated
     */
    public void setScript(@Nonnull final EvaluableScript matcherScript) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        script = Constraint.isNotNull(matcherScript, "Attribute value matching script cannot be null");
    }

    /** {@inheritDoc} */
    @Override public void run() {
        scriptEvaluator.execute();
    }

    /**
     * The thing that runs the script.
     */
    private class RunnableScriptEvaluator extends AbstractScriptEvaluator {

        /**
         * Constructor.
         * 
         * @param theScript the script we will evaluate.
         */
        public RunnableScriptEvaluator(@Nonnull final EvaluableScript theScript) {
            super(theScript);
        }

        /** {@inheritDoc} */
        @Override protected void prepareContext(final ScriptContext scriptContext, final Object... input) {
            // Nothing to do
        }

        /**
         *  Run the script.  Logging as appropriate.
         */
        public void execute() {
            log.debug("{}: running script", getLogPrefix());
            evaluate((Object[]) null);
        }
    }
}
