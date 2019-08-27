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

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * A component that evaluates a Spring EL expression against a set of inputs
 * and returns the result.
 * 
 * @param <T> type of input
 * @param <U> type of output
 * 
 * @since 5.4.0
 */
public abstract class AbstractSpringExpressionEvaluator<T, U> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractSpringExpressionEvaluator.class);

    /** SpEL expression to evaluate. */
    @Nullable private String springExpression;
    
    /** A custom object to inject into the expression context. */
    @Nullable private Object customObject;
    
    /** The output type. */
    @Nullable private Class<U> outputType;

    /** The input type. */
    @Nullable private Class<T> inputType;


    /** Whether to raise runtime exceptions if expression fails. */
    private boolean hideExceptions;
    
    /** Value to return from predicate when an error occurs. */
    private U returnOnError;

    /**
     * Constructor.
     *
     * @param expression the expression to evaluate
     */
    public AbstractSpringExpressionEvaluator(
            final @Nonnull @NotEmpty @ParameterName(name="expression") String expression) {
        springExpression = Constraint.isNotNull(StringSupport.trimOrNull(expression),
                "Expression cannot be null or empty");
    }
    
    /**
     * Set the output type to be enforced.
     * 
     * @param type output type
     */
    public void setOutputType(@Nullable final Class<U> type) {
        outputType = type;
    }

    /**
     * Get the output type to be enforced.
     * 
     * @return output type
     */
    @Nullable protected Class<U> getOutputType() {
        return outputType;
    }

    /**
     * Get the input type to be enforced.
     *
     * @return input type
     */
    @Nullable public  Class<T> getInputType() {
        return inputType;
    }

    /**
     * Set the input type to be enforced.
     *
     * @param type input type
     */
    public void setInputType(@Nullable final Class<T> type) {
        inputType = type;
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
     * Get the custom (externally provided) object.
     *
     * @return the custom object
     */
    public Object getCustomObject() {
        return customObject;
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
     * Set value to return if an error occurs (default is false).
     * 
     * @param what value to set
     */
    public void setReturnOnError(final U what) {
        returnOnError = what;
    }

    /**
     * Evaluate the Spring expression on the provided input.
     *
     * @param input input over which to evaluate the expression
     * @return result of applying the expression to the provided input
     */
    protected U evaluate(@Nullable final T input) {

        // Try outside the try so as to preserve derived semantics
        if (null != input && null != getInputType() && !getInputType().isInstance(input)) {
            log.error("Input was type {} which is not an instance of {}",  input.getClass(), getInputType());
            throw new ClassCastException("Input was type " + input.getClass() + " which is not an instance of "
                    + getInputType());
        }

        try {
            final ExpressionParser parser = new SpelExpressionParser();
            final StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("custom", customObject);
            context.setVariable("input", input);
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
            
            return (U) output;
        } catch (final Exception e) {
            log.error("Error evaluating Spring expression", e);
            if (hideExceptions) {
                return returnOnError;
            }
            throw e;
        }
    }
    
    /**
     * Decorate the expression context with any additional content.
     * 
     * @param context expression context
     * @param input to predicate
     */
     protected void prepareContext(@Nonnull final EvaluationContext context, @Nullable final T input) {
        
    }

}