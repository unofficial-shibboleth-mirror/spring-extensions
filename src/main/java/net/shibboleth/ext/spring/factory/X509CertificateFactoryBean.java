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
import java.security.Security;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;

import edu.vt.middleware.crypt.util.CryptReader;

/**
 * Spring bean factory for producing a {@link X509Certificate} from a file.
 * 
 * This factory bean supports DER and PEM encoded certificate files.
 */
public class X509CertificateFactoryBean implements FactoryBean<X509Certificate> {

    /** Certificate chain file. */
    private File certFile;

    /** The singleton instance of the certificate produced by this factory. */
    private X509Certificate certificate;

    /**
     * Sets the certificate chain file.
     * 
     * @param file certificate chain file
     */
    public void setCertificateFile(@Nonnull final File file) {
        certFile = Constraint.isNotNull(file, "Certificate file can not be null");
    }

    /** {@inheritDoc} */
    public X509Certificate getObject() throws Exception {
        if (certificate == null) {
            if (certFile == null) {
                throw new BeanCreationException("Certificate file must be provided in order to use this factory.");
            }

            Security.addProvider(new BouncyCastleProvider());
            certificate = (X509Certificate) CryptReader.readCertificate(new FileInputStream(certFile));
        }

        return certificate;
    }

    /** {@inheritDoc} */
    @Nonnull public Class<?> getObjectType() {
        return X509Certificate.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return true;
    }
}