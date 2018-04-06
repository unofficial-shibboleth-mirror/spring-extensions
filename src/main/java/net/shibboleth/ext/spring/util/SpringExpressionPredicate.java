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

import com.google.common.base.Predicate;

/**
 * Predicate whose condition is defined by an Spring EL expression.
 * 
 * @param <T> type of input
 * 
 * @since 5.4.0
 */
public class SpringExpressionPredicate<T> implements Predicate<T> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SpringExpressionPredicate.class);

    /** SpEL expression to evaluate. */
    @Nullable private String springExpression;
    
    /** A custom object to inject into the expression context. */
    @Nullable private Object customObject;

    /** Whether to raise runtime exceptions if expression fails. */
    private boolean hideExceptions;
    
    /** Value to return from predicate when an error occurs. */
    private boolean returnOnError;

    /**
     * Constructor.
     *
     * @param expression the expression to evaluate
     */
    public SpringExpressionPredicate(@Nonnull @NotEmpty @ParameterName(name="expression") final String expression) {
        springExpression = Constraint.isNotNull(StringSupport.trimOrNull(expression),
                "Expression cannot be null or empty");
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
     * Set value to return if an error occurs (default is false).
     * 
     * @param flag flag to set
     */
    public void setReturnOnError(final boolean flag) {
        returnOnError = flag;
    }

    /** {@inheritDoc} */
    @Override
    public boolean apply(@Nullable final T input) {

        try {
            final ExpressionParser parser = new SpelExpressionParser();
            final StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("custom", customObject);
            context.setVariable("input", input);
            prepareContext(context, input);
            return parser.parseExpression(springExpression).getValue(context, Boolean.class);
        } catch (final ParseException|EvaluationException e) {
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