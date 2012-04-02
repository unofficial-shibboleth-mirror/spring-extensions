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

import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.Security;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;

import edu.vt.middleware.crypt.util.CryptReader;

/**
 * Spring bean factory for producing a {@link PrivateKey} from a file.
 * 
 * This factory bean supports encrypted and non-encrypted PKCS8, DER, or PEM private key encoded files.
 */
public class PrivateKeyFactoryBean implements FactoryBean<PrivateKey> {

    /** Private key file. */
    private File keyFile;

    /** Password for the private key. */
    private String keyPass;

    /** The singleton instance of the private key produced by this factory. */
    private PrivateKey key;

    /**
     * Sets the file containing the private key.
     * 
     * @param file private key file, never null
     */
    public void setPrivateKeyFile(@Nonnull final File file) {
        keyFile = Constraint.isNotNull(file, "Private key file can not be null");
    }

    /**
     * Sets the password for the private key.
     * 
     * @param password password for the private key, may be null if the key is not encrypted
     */
    public void setPrivateKeyPassword(@Nullable final String password) {
        keyPass = StringSupport.trimOrNull(password);
    }

    /** {@inheritDoc} */
    public PrivateKey getObject() throws Exception {
        if (key == null) {
            if (keyFile == null) {
                throw new BeanCreationException("Private key file must be provided in order to use this factory.");
            }

            Security.addProvider(new BouncyCastleProvider());
            if (keyPass == null) {
                key = CryptReader.readPrivateKey(new FileInputStream(keyFile));
            } else {
                key = CryptReader.readPrivateKey(new FileInputStream(keyFile), keyPass.toCharArray());
            }
        }

        return key;
    }

    /** {@inheritDoc} */
    @Nonnull public Class<?> getObjectType() {
        return PrivateKey.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return true;
    }
}