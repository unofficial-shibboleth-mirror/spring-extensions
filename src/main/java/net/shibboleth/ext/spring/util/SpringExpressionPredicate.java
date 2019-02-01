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
import net.shibboleth.utilities.java.support.logic.Predicate;

/**
 * Predicate whose condition is defined by an Spring EL expression.
 * 
 * @param <T> type of input
 * 
 * @since 5.4.0
 */
public class SpringExpressionPredicate<T> extends AbstractSpringExpressionEvaluator<T, Boolean> 
            implements Predicate<T> {
    
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
     * Set value to return if an error occurs (default is false).
     * 
     * @param flag flag to set
     */
    public void setReturnOnError(final boolean flag) {
        super.setReturnOnError(flag);
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final T input) {
        return evaluate(input);
    }
    
}