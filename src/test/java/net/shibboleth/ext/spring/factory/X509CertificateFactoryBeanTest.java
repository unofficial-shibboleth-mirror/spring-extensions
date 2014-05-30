
package net.shibboleth.ext.spring.factory;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.Assert;
import org.testng.annotations.Test;

@ContextConfiguration({"X509CertificateFactoryBean-config.xml"})
public class X509CertificateFactoryBeanTest extends AbstractTestNGSpringContextTests {

    @Test public void testFactory() {
        final Object bean = applicationContext.getBean("certificate");
        Assert.notNull(bean);
        Assert.isInstanceOf(X509Certificate.class, bean);
        final X509Certificate cert = (X509Certificate)bean;
        final PublicKey pubkey = cert.getPublicKey();
        Assert.notNull(pubkey);
        Assert.isInstanceOf(RSAPublicKey.class, pubkey);
        final RSAPublicKey rsaKey = (RSAPublicKey)pubkey;
        Assert.isTrue(rsaKey.getModulus().bitLength() == 2048);
    }

}
