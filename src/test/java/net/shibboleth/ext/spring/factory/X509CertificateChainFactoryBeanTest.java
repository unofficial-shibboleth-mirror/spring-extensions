
package net.shibboleth.ext.spring.factory;

import java.security.cert.X509Certificate;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.Assert;
import org.testng.annotations.Test;

@ContextConfiguration({"X509CertificateChainFactoryBean-config.xml"})
public class X509CertificateChainFactoryBeanTest extends AbstractTestNGSpringContextTests {

    @Test public void testFactory() {
        final Object bean = applicationContext.getBean("chain");
        Assert.notNull(bean);
        final X509Certificate[] chain = (X509Certificate[])bean;
        Assert.isTrue(chain.length == 3);
    }

}
