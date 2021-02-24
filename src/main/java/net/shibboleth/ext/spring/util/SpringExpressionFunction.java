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

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Function whose output is defined by an Spring EL expression.
 * 
 * @param <T> type of input
 * @param <U> type of output
 * 
 * @since 5.4.0
 */
public class SpringExpressionFunction<T,U> extends AbstractSpringExpressionEvaluatorEx
            implements Function<T,U> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SpringExpressionFunction.class);

    /** The input type. */
    @Nullable private Class<T> inputType;

    /**
     * Constructor.
     *
     * @param expression the expression to evaluate
     */
    public SpringExpressionFunction(@Nonnull @NotEmpty @ParameterName(name="expression") final String expression) {
        super(expression);
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
     * Set the output type to be enforced.
     *
     * @param type output type
     */
    public void setOutputType(@Nullable final Class<?> type) {
        super.setOutputType(type);
    }

    /**
     * Set value to return if an error occurs.
     *
     * @param value value to return
     */
    @Override public void setReturnOnError(@Nullable final Object value) {
        super.setReturnOnError(value);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public U apply(@Nullable final T input) {
        
        // Try outside the try so as to preserve derived semantics
        if (null != input && null != getInputType() && !getInputType().isInstance(input)) {
            log.error("Input was type {} which is not an instance of {}",  input.getClass(), getInputType());
            throw new ClassCastException("Input was type " + input.getClass() + " which is not an instance of "
                    + getInputType());
        }

        return (U) evaluate(input);
    }

    /** {@inheritDoc} */
    @Override
    protected void prepareContext(@Nonnull final EvaluationContext context, @Nullable final Object... input) {
        context.setVariable("input", input[0]);
    }
    
}