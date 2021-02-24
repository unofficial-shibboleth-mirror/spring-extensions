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
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A component that evaluates a Spring EL expression against a set of inputs
 * and returns the result.
 * 
 * @since 6.1.0
 */
public abstract class AbstractSpringExpressionEvaluatorEx {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSpringExpressionEvaluatorEx.class);

    /** SpEL expression to evaluate. */
    @Nullable private String springExpression;
    
    /** A custom object to inject into the expression context. */
    @Nullable private Object customObject;
    
    /** The output type. */
    @Nullable private Class<?> outputType;

    /** Whether to raise runtime exceptions if expression fails. */
    private boolean hideExceptions;
    
    /** Value to return from predicate when an error occurs. */
    @Nullable private Object returnOnError;

    /**
     * Constructor.
     *
     * @param expression the expression to evaluate
     */
    public AbstractSpringExpressionEvaluatorEx(
            final @Nonnull @NotEmpty @ParameterName(name="expression") String expression) {
        springExpression = Constraint.isNotNull(StringSupport.trimOrNull(expression),
                "Expression cannot be null or empty");
    }
    
    /**
     * Get the output type to be enforced.
     * 
     * @return output type
     */
    @Nullable protected Class<?> getOutputType() {
        return outputType;
    }

    /**
     * Set the output type to be enforced.
     * 
     * @param type output type
     */
    protected void setOutputType(@Nullable final Class<?> type) {
        outputType = type;
    }

    /**
     * Get the custom (externally provided) object.
     *
     * @return the custom object
     */
    @Nullable protected Object getCustomObject() {
        return customObject;
    }

    /**
     * Set a custom (externally provided) object.
     * 
     * @param object the custom object
     */
    public void setCustomObject(@Nullable final Object object) {
        customObject = object;
    }

    /**
     * Set whether to hide exceptions in expression execution (default is false).
     * 
     * @param flag flag to set
     */
    public void setHideExceptions(final boolean flag) {
        hideExceptions = flag;
    }

    /**
     * Get value to return if an error occurs.
     * 
     * @return value to return
     */
    @Nullable protected Object getReturnOnError() {
        return returnOnError;
    }
    
    /**
     * Set value to return if an error occurs.
     * 
     * @param value value to return
     */
    protected void setReturnOnError(@Nullable final Object value) {
        returnOnError = value;
    }

    /**
     * Evaluate the Spring expression on the provided input.
     *
     * @param input input arguments
     * 
     * @return result of applying the expression to the provided inputs
     */
    @Nullable protected Object evaluate(@Nullable final Object... input) {

        try {
            final ExpressionParser parser = new SpelExpressionParser();
            final StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("custom", customObject);
            prepareContext(context, input);
            final Object output = parser.parseExpression(springExpression).getValue(context);

            if (output == null) {
                return null;
            }
            
            if (null != getOutputType()) {
                if (!getOutputType().isInstance(output)) {
                    log.error("Output of type {} was not of type {}", output.getClass(), getOutputType());
                    return returnOnError;
                }
                
                return getOutputType().cast(output);
            }
            
            return output;
        } catch (final Exception e) {
            log.error("Error evaluating Spring expression", e);
            if (hideExceptions) {
                return returnOnError;
            }
            throw e;
        }
    }
    
    /**
     * Pre-process the script context before execution.
     * 
     * @param context the expression context
     * @param input the inputs
     */
     protected abstract void prepareContext(@Nonnull final EvaluationContext context, @Nullable final Object... input);

}