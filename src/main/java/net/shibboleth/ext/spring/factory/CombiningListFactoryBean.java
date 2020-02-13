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

package net.shibboleth.ext.spring.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ListFactoryBean;

import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A factory which extends {@link ListFactoryBean} by requiring two
 * lists as input.
 */
public class CombiningListFactoryBean extends ListFactoryBean {

    /** First list to combine. */
    private List<?> firstList = Collections.emptyList();
 
    /** Second list to combine. */
    private List<?> secondList = Collections.emptyList();
 
    /** {@inheritDoc} */
    @Override public void setSourceList(final List<?> sourceList) {
        throw new BeanCreationException("Call setFirstList() amnd setSecondList");
    }

    /** Set the first list to combine.
     * @return Returns the firstList.
     */
    public List<?> getFirstList() {
        return firstList;
    }

    /** Get the first list to combine.
     * @param list The firstList to set.
     */
    public void setFirstList(@Nonnull final List<?> list) {
        firstList = Constraint.isNotNull(list, "First list must be nonnull");
    }

    /** Set the second list to combine.
     * @return Returns the secondList.
     */
    public List<?> getSecondList() {
        return secondList;
    }

    /** Get the second list to combine.
     * @param list The secondList to set.
     */
    public void setSecondList(@Nonnull final List<?> list) {
        secondList = Constraint.isNotNull(list, "Second list must be nonnull");
    }
    
    /** {@inheritDoc} */
    @Override protected List<Object> createInstance() {
        final ArrayList<Object> combined = new ArrayList<>(firstList.size() + secondList.size());
        combined.addAll(firstList);
        combined.addAll(secondList);
        super.setSourceList(combined);
        return super.createInstance();
    }

}
