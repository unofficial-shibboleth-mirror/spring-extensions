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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

@ContextConfiguration({"X509CertificateFactoryBean-config.xml"})
@SuppressWarnings("javadoc")
public class X509CertificateFactoryBeanTest extends AbstractTestNGSpringContextTests {

    @Test public void testFactory() {
        final Object bean = applicationContext.getBean("certificate");
        Assert.assertNotNull(bean);
        Assert.assertTrue(bean instanceof X509Certificate);
        final X509Certificate cert = (X509Certificate)bean;
        final PublicKey pubkey = cert.getPublicKey();
        Assert.assertNotNull(pubkey);
        Assert.assertTrue(pubkey instanceof RSAPublicKey);
        final RSAPublicKey rsaKey = (RSAPublicKey)pubkey;
        Assert.assertTrue(rsaKey.getModulus().bitLength() == 2048);
    }

}
