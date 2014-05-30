
package net.shibboleth.ext.spring.factory;

import java.security.interfaces.RSAPublicKey;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.Assert;
import org.testng.annotations.Test;

@ContextConfiguration({"PublicKeyFactoryBean-config.xml"})
public class PublicKeyFactoryBeanTest extends AbstractTestNGSpringContextTests {

    @Test public void testFactory() {
        final Object bean = applicationContext.getBean("key");
        Assert.notNull(bean);
        Assert.isInstanceOf(RSAPublicKey.class, bean);
        final RSAPublicKey rsaKey = (RSAPublicKey)bean;
        Assert.isTrue(rsaKey.getModulus().bitLength() == 2048);
    }

}
