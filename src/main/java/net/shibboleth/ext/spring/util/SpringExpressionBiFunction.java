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

import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;

/**
 * Predicate whose condition is defined by an Spring EL expression.
 * 
 * @param <T> first input type
 * @param <U> second input type
 * @param <V> return type
 * 
 * @since 6.1.0
 */
public class SpringExpressionBiFunction<T,U,V> extends AbstractSpringExpressionEvaluatorEx 
            implements BiFunction<T,U,V> {
    
    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SpringExpressionBiFunction.class);

    /** Input type 1. */
    @Nullable private Class<T> inputTypeClass1;

    /** Input type 2. */
    @Nullable private Class<U> inputTypeClass2;

    /**
     * Constructor.
     *
     * @param expression the expression to evaluate
     */
    public SpringExpressionBiFunction(@Nonnull @NotEmpty @ParameterName(name="expression") final String expression) {
        super(expression);
    }

    /**
     * Get the input type to be enforced.
     *
     * @return input type
     */
    @Nullable public Pair<Class<T>,Class<U>> getInputTypes() {
        return new Pair<>(inputTypeClass1, inputTypeClass2);
    }

    /**
     * Set the input type to be enforced.
     *
     * @param type1 first input type
     * @param type2 second input type
     */
    public void setInputTypes(@Nullable final Class<T> type1, @Nullable final Class<U> type2) {
        inputTypeClass1 = type1;
        inputTypeClass2 = type2;
    }

    /**
     * Set the output type to be enforced.
     *
     * @param type output type
     */
    @Override public void setOutputType(@Nullable final Class<?> type) {
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
    @Nullable public V apply(@Nullable final T first, @Nullable final U second) {
        final Pair<Class<T>,Class<U>> types = getInputTypes();
        if (null != types) {
            if (null != first && !types.getFirst().isInstance(first)) {
                log.error("Input of type {} was not of type {}", first.getClass(), types.getFirst());
                return (V) getReturnOnError();
            }
            if (null != second && !types.getSecond().isInstance(second)) {
                log.error("Input of type {} was not of type {}", second.getClass(),
                        types.getSecond());
                return (V) getReturnOnError();
            }
        }

        return (V) evaluate(first, second);
    }

    /** {@inheritDoc} */
    @Override
    protected void prepareContext(@Nonnull final EvaluationContext context, @Nullable final Object... input) {
        context.setVariable("input1", input[0]);
        context.setVariable("input2", input[1]);
    }
    
}