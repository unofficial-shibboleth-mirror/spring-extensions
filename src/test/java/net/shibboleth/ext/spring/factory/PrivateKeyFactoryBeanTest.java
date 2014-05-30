
package net.shibboleth.ext.spring.factory;

import java.security.interfaces.RSAPrivateKey;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.Assert;
import org.testng.annotations.Test;

@ContextConfiguration({"PrivateKeyFactoryBean-config.xml"})
public class PrivateKeyFactoryBeanTest extends AbstractTestNGSpringContextTests {

    @Test public void testFactory() {
        final Object bean = applicationContext.getBean("key");
        Assert.notNull(bean);
        Assert.isInstanceOf(RSAPrivateKey.class, bean);
        final RSAPrivateKey rsaKey = (RSAPrivateKey)bean;
        Assert.isTrue(rsaKey.getModulus().bitLength() == 2048);
    }

}
