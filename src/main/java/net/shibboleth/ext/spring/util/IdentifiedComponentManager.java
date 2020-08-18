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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.IdentifiedComponent;

/**
 * Class used for auto-wiring free-standing identified objects along with explicitly declared
 * bean collections.
 * 
 * <p>This class marries old-style "explicit" list-based configuration of a collection of
 * objects with annotation-driven discovery of objects of the same type.</p>
 * 
 * <p>Specializations of this class are expected to fix the type and add an Autowired
 * constructor to receive the free-standing objects.</p>
 * 
 * @param <T> descriptor type
 */
public class IdentifiedComponentManager<T extends IdentifiedComponent> {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(IdentifiedComponentManager.class);
    
    /** Underlying collection. */
    @Nonnull @NonnullElements private Collection<T> components;
    
    /**
     * Auto-wiring point for free-standing objects.
     * 
     * @param freeObjects free-standing objects
     */
    public IdentifiedComponentManager(@Nullable @NonnullElements final Collection<T> freeObjects) {
        if (freeObjects != null) {
            components = List.copyOf(freeObjects);
        } else {
            components = Collections.emptyList();
        }
    }
    
    /**
     * Sets additional non-autowired components to merge in.
     * 
     * <p>For now, this set is prepended to any auto-wired objects and any auto-wired objects
     * are excluded if they have the same identifier as an explicitly injected object.</p>
     * 
     * @param additionalObjects additional objects
     */
    public void setComponents(@Nullable @NonnullElements final Collection<T> additionalObjects) {
        if (additionalObjects != null) {
            final Collection<T> holder = new LinkedHashSet<>(additionalObjects);
            holder.addAll(
                    components.stream()
                        .filter(obj -> {
                            if (holder.contains(obj)) {
                                log.info("Replacing auto-wired component: {}", obj.getId());
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toUnmodifiableList()));
            components = List.copyOf(holder);
        }
    }
    
    /**
     * Gets the final collection of merged components.
     * 
     * @return merged components
     */
    @Nonnull @NonnullElements public Collection<T> getComponents() {
        return components;
    }
    
}