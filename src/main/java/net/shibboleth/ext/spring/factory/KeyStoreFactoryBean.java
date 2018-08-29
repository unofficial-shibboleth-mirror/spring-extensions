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
import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.core.io.Resource;

/**
 * Spring bean factory for producing a {@link KeyStore} from a {@link Resource}.
 * 
 * @since 5.4.0
 */
public class KeyStoreFactoryBean implements FactoryBean<KeyStore> {

    /** KeyStore resource. */
    @Nullable private Resource resource;

    /** Password for the keystore. */
    @Nullable private String keyPass;

    /** KeyStore type. */
    @Nullable private String type;

    /** KeyStore provider. */
    @Nullable private String provider;
    
    /** The singleton instance of the private key produced by this factory. */
    @Nullable private KeyStore keyStore;

    /**
     * Set the resource containing the keystore.
     * 
     * @param res private key resource
     */
    public void setResource(@Nonnull final Resource res) {
        resource = Constraint.isNotNull(res, "KeyStore resource cannot be null");
    }

    /**
     * Set the password for the keystore.
     * 
     * @param password password for the keystore
     */
    public void setPassword(@Nullable final String password) {
        keyPass = password;
    }

    /**
     * Set the KeyStore provider, if non-defaulted.
     * 
     * @param prov provider
     */
    public void setProvider(@Nullable final String prov) {
        provider = StringSupport.trimOrNull(prov);
    }
    
    /**
     * Set the KeyStore type, if non-defaulted.
     * 
     * @param typ KeyStore type
     */
    public void setType(@Nullable final String typ) {
        type = StringSupport.trimOrNull(typ);
    }
    
    /** {@inheritDoc} */
    @Override public KeyStore getObject() throws Exception {

        if (keyStore == null) {
            if (resource == null) {
                throw new FactoryBeanNotInitializedException("Resource property cannot be null");
            }
            
            if (provider != null && type != null) {
                keyStore = KeyStore.getInstance(type, provider);
            } else if (type != null) {
                keyStore = KeyStore.getInstance(type);
            } else {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            }
            try (final InputStream is = resource.getInputStream()) {
                keyStore.load(is, keyPass.toCharArray());
            }
        }

        return keyStore;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public Class<?> getObjectType() {
        return KeyStore.class;
    }

    /** {@inheritDoc} */
    @Override public boolean isSingleton() {
        return true;
    }
}