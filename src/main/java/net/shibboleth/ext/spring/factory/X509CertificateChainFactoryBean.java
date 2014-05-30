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
import java.io.InputStream;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;

import org.cryptacular.util.CertUtil;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring bean factory for producing a {@link X509Certificate} chains from a file.
 * 
 * This factory bean supports DER and PEM encoded certificate files.
 */
public class X509CertificateChainFactoryBean implements FactoryBean<X509Certificate[]> {

    /** Certificate chain file. */
    private File certChainFile;

    /** The singleton instance of the public certificate chain produced by this factory. */
    private X509Certificate[] certificates;

    /**
     * Sets the certificate chain file.
     * 
     * @param file certificate chain file
     */
    public void setCertificateChainFile(@Nonnull final File file) {
        certChainFile = Constraint.isNotNull(file, "Certificate chain file can not be null");
    }

    /** {@inheritDoc} */
    public X509Certificate[] getObject() throws Exception {
        if (certificates == null) {
            if (certChainFile == null) {
                throw new BeanCreationException(
                        "Certificate chanin file must be provided in order to use this factory.");
            }

            try (InputStream is = new FileInputStream(certChainFile)) {
                certificates = (X509Certificate[]) CertUtil.readCertificateChain(is);
            }
        }

        return certificates;
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