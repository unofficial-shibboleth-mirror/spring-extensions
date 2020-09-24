
package net.shibboleth.ext.spring.factory;

import java.security.Security;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test class for {@link PKCS11PrivateKeyFactoryBean}.
 *
 * <p>
 * It's really hard to provide tests for this bean that will run anywhere, because
 * by definition it requires access to a PKCS#11 token, which is not something you'll
 * find on any street corner.
 * </p>
 * 
 * <p>
 * The approach taken here is to have one test for known prerequisites which should be
 * true in all environments, and a number of (by default <em>disabled</em> tests
 * exercising specific scenarios with specific tokens.
 * </p>
 */
public class PKCS11PrivateKeyFactoryBeanTest {

    /**
     * Test universal prerequisites.
     *
     * @throws Exception if something bad happens
     */
    @Test
    public void testPrerequisites() throws Exception {
        final var p = Security.getProvider("SunPKCS11");
        Assert.assertNotNull(p);
    }

    /**
     * Test a specific SoftHSM deployment on Ian's desktop
     * Mac.
     *
     * <p>
     * Disabled by default because our normal test environment doesn't
     * include this setup.
     * </p>
     *
     * @throws Exception if something bad happens
     */
    @Test(enabled = false)
    public void testSoftHSM() throws Exception {
        // Locate the configuration file
        final var configResource =
                this.getClass().getResource("/net/shibboleth/ext/spring/factory/PKCS11PrivateKeyFactoryBean-softhsm.cfg");
        final var configFile = configResource.getFile();

        // Wire up the factory bean
        final var fac = new PKCS11PrivateKeyFactoryBean();
        fac.setPkcs11Config(configFile);
        fac.setKeyPassword("1234");
        fac.setKeyAlias("key2048");
        
        // Fetch the private key from the resulting keystore
        final var key = fac.getObject();
        Assert.assertNotNull(key);
        
        // Dig a little deeper. The key can't be extracted, but we can
        // look at the algorithm.
        Assert.assertEquals(key.getAlgorithm(), "RSA");
    }

}
