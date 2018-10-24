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

package net.shibboleth.ext.spring.velocity;

import javax.annotation.Nonnull;

import org.apache.velocity.app.VelocityEngine;

/**
 * Interface to be implemented by objects that configure and manage a
 * VelocityEngine for automatic lookup in a web environment. Detected
 * and used by {@link VelocityView}.
 *
 * @author Rod Johnson
 * 
 * @since 6.0.0
 */
public interface VelocityConfig {

     /**
      * Return the VelocityEngine for the current web application context.
      * 
      * <p>May be unique to one servlet, or shared in the root context.</p>
      * 
      * @return the VelocityEngine
      */
     @Nonnull VelocityEngine getVelocityEngine();
}