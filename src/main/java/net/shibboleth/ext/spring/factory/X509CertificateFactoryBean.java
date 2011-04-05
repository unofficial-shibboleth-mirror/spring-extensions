/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
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

    /**
     * Sets the certificate chain file.
     * 
     * @param file certificate chain file, never null
     */
    public void setCertificateFile(final File file) {
        certFile = file;
    }

    /** {@inheritDoc} */
    public X509Certificate getObject() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        return (X509Certificate) CryptReader.readCertificate(new FileInputStream(certFile));
    }

    /** {@inheritDoc} */
    public Class<?> getObjectType() {
        return X509Certificate.class;
    }

    /** {@inheritDoc} */
    public boolean isSingleton() {
        return true;
    }
}