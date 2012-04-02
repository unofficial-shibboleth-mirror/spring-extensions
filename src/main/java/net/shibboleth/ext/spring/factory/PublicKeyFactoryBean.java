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
import java.security.PublicKey;
import java.security.Security;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;

import edu.vt.middleware.crypt.util.CryptReader;

/**
 * Spring bean factory for producing a {@link PublicKey} from a file.
 * 
 * This factory bean supports DER and PEM encoded public key files.
 */
public class PublicKeyFactoryBean implements FactoryBean<PublicKey> {

    /** Public key file. */
    private File keyFile;

    /** The singleton instance of the public key produced by this factory. */
    private PublicKey key;

    /**
     * Sets the public key file.
     * 
     * @param file public key file
     */
    public void setPublicKeyFile(@Nonnull final File file) {
        keyFile = Constraint.isNotNull(file, "Public key file can not be null");
    }

    /** {@inheritDoc} */
    public PublicKey getObject() throws Exception {
        if (key == null) {
            if (keyFile == null) {
                throw new BeanCreationException("Public key file must be provided in order to use this factory.");
            }

            Security.addProvider(new BouncyCastleProvider());
            key = CryptReader.readPublicKey(new FileInputStream(keyFile));
        }

        return key;
    }

    /** {@inheritDoc} */
    @Nonnull public Class<?> getObjectType() {
        return PublicKey.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return true;
    }
}