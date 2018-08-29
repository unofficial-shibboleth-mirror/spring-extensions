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

import java.lang.reflect.Constructor;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring bean factory for extracting a {@link PrivateKey} from a PKCS#11 keystore.
 * 
 * This relies on the SunPKCS11 provider.
 */
public class PKCS11PrivateKeyFactoryBean implements FactoryBean<PrivateKey> {

    /** The class name for the PKCS#11 provider class. */
    private static final String PROVIDER_CLASS_NAME = "sun.security.pkcs11.SunPKCS11";

    /** Singleton {@link Provider} for all instances of this factory. */
    private static Provider provider;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PKCS11PrivateKeyFactoryBean.class);

    /** PKCS#11 provider parameter string. */
    private String pkcs11Config;

    /** Alias for the private key. */
    private String keyAlias;

    /** Password for the private key. */
    private String keyPassword;

    /** The singleton instance of the private key produced by this factory. */
    private PrivateKey key;

    /**
     * Returns the PKCS#11 configuration.
     * 
     * @return returns the PKCS#11 configuration.
     */
    public String getPkcs11Config() {
        return pkcs11Config;
    }

    /**
     * Sets the PKCS#11 configuration to use.
     * 
     * @param config the PKCS#11 configuration to use
     */
    public void setPkcs11Config(@Nonnull final String config) {
        pkcs11Config = config;
    }

    /**
     * Gets the key alias in use.
     * 
     * @return returns the key alias in use
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Sets the key alias to use.
     * 
     * @param alias the key alias to use
     */
    public void setKeyAlias(final String alias) {
        keyAlias = alias;
    }

    /**
     * Gets the key password in use.
     * 
     * @return returns the key password in use
     */
    public String getKeyPassword() {
        return keyPassword;
    }

    /**
     * Set the key password to use.
     * 
     * @param password the key password to use
     */
    public void setKeyPassword(@Nonnull final String password) {
        keyPassword = password;
    }

    /**
     * Gets the singleton PKCS#11 {@link Provider}.
     * 
     * The constructed {@link Provider} is also added to the system's list of providers.
     * 
     * @return the singleton {@link Provider}
     * @throws Exception if something goes wrong building the {@link Provider}
     */
    private Provider getProvider() throws Exception {
        if (provider == null) {
            final ClassLoader loader = PKCS11PrivateKeyFactoryBean.class.getClassLoader();
            try {
                final Class<Provider> providerClass = (Class<Provider>) loader.loadClass(PROVIDER_CLASS_NAME);
                final Constructor<Provider> providerConstructor = providerClass.getConstructor(String.class);
                provider = providerConstructor.newInstance(pkcs11Config);
                Security.addProvider(provider);
            } catch (final ClassNotFoundException e) {
                throw new NoSuchProviderException("unable to load keystore provider class " + PROVIDER_CLASS_NAME);
            } catch (final NoSuchMethodException e) {
                throw new NoSuchProviderException("keystore provider class " + PROVIDER_CLASS_NAME
                        + " does not provide a String-argument constructor ");
            }
        }
        return provider;
    }

    /**
     * Gets a PKCS#11 {@link KeyStore} from the {@link Provider}.
     * 
     * @return the {@link KeyStore}
     * @throws Exception if something goes wrong building the keystore
     */
    private KeyStore getKeyStore() throws Exception {
        final KeyStore keystore = KeyStore.getInstance("PKCS11", getProvider());

        log.debug("Initializing PKCS11 keystore");
        keystore.load(null, keyPassword.toCharArray());
        return keystore;
    }

    @Override
    public PrivateKey getObject() throws Exception {
        if (key == null) {
            final KeyStore keystore = getKeyStore();

            final KeyStore.Entry keyEntry = keystore.getEntry(keyAlias,
                    new KeyStore.PasswordProtection(keyPassword.toCharArray()));
            if (keyEntry == null) {
                throw new GeneralSecurityException("entry " + keyAlias + " not found");
            }
            
            if (keyEntry instanceof PrivateKeyEntry) {
                final PrivateKeyEntry privKeyEntry = (PrivateKeyEntry) keyEntry;
                key = privKeyEntry.getPrivateKey();
            } else {
                throw new GeneralSecurityException("entry " + keyAlias + " is not a private key entry");
            }
        }

        return key;
    }

    @Override
    @Nonnull public Class<?> getObjectType() {
        return PrivateKey.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
