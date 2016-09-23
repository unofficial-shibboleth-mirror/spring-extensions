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

import java.io.InputStream;
import java.security.PublicKey;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.cryptacular.util.KeyPairUtil;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

/**
 * Spring bean factory for producing a {@link PublicKey} from a {@link Resource}.
 * 
 * This factory bean supports DER and PEM encoded public key resources.
 */
public class PublicKeyFactoryBean implements FactoryBean<PublicKey> {

    /** Public key resource. */
    private Resource resource;

    /** The singleton instance of the public key produced by this factory. */
    private PublicKey key;

    /**
     * Sets the public key resource.
     * 
     * @param res public key resource
     */
    public void setResource(@Nonnull final Resource res) {
        resource = Constraint.isNotNull(res, "Public key resource can not be null");
    }

    /** {@inheritDoc} */
    @Override public PublicKey getObject() throws Exception {
        if (key == null) {
            if (resource == null) {
                throw new BeanCreationException("Public key resource must be provided in order to use this factory.");
            }

            try (InputStream is = resource.getInputStream()) {
                key = KeyPairUtil.readPublicKey(is);
            }
        }

        return key;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public Class<?> getObjectType() {
        return PublicKey.class;
    }

    /** {@inheritDoc} */
    @Override public boolean isSingleton() {
        return true;
    }
}