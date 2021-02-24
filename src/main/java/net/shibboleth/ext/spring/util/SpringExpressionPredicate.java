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

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Predicate;
import net.shibboleth.utilities.java.support.logic.ScriptedPredicate;

/**
 * Predicate whose condition is defined by an Spring EL expression.
 * 
 * @param <T> type of input
 * 
 * @since 5.4.0
 */
public class SpringExpressionPredicate<T> extends AbstractSpringExpressionEvaluatorEx 
            implements Predicate<T> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedPredicate.class);

    /** Input type. */
    @Nullable private Class<T> inputTypeClass;

    /**
     * Constructor.
     *
     * @param expression the expression to evaluate
     */
    public SpringExpressionPredicate(@Nonnull @NotEmpty @ParameterName(name="expression") final String expression) {
        super(expression);
        setOutputType(Boolean.class);
    }

    /**
     * Get the input type to be enforced.
     *
     * @return input type
     * 
     * @since 6.1.0
     */
    @Nullable public Class<T> getInputType() {
        return inputTypeClass;
    }

    /**
     * Set the input type to be enforced.
     *
     * @param type input type
     * 
     * @since 6.1.0
     */
    public void setInputType(@Nullable final Class<T> type) {
        inputTypeClass = type;
    }
    
    /**
     * Set value to return if an error occurs (default is false).
     * 
     * @param flag flag to set
     */
    public void setReturnOnError(final boolean flag) {
        super.setReturnOnError(flag);
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final T input) {
        if (null != getInputType() && null != input && !getInputType().isInstance(input)) {
            log.error("Input of type {} was not of type {}", input.getClass(), getInputType());
            return (boolean) getReturnOnError();
        }

        final Object result = evaluate(input);
        return (boolean) (result != null ? result : getReturnOnError());
    }

    /** {@inheritDoc} */
    @Override
    protected void prepareContext(@Nonnull final EvaluationContext context, @Nullable final Object... input) {
        context.setVariable("input", input[0]);
    }
    
}