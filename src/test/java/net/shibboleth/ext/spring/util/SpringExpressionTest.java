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

package net.shibboleth.ext.spring.util;

import java.util.HashMap;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for the {@link SpringExpressionPredicate} and {@link SpringExpressionFunction}.
 */
@SuppressWarnings("javadoc")
public class SpringExpressionTest {

    /** Helper function to use this as the bean to test.
     * @return 99
     */
    public int getValue99() {
        return 99;
    }
     
    @Test public void testPredicates() {
        
        SpringExpressionPredicate<SpringExpressionTest> predicate = new SpringExpressionPredicate<>("#input.getValue99() == 99");
        predicate.setInputType(SpringExpressionTest.class);
        
        Assert.assertTrue(predicate.test(this));
        
        predicate = new SpringExpressionPredicate<>("#input.getValue99() == #custom");
        predicate.setCustomObject(99);
        Assert.assertTrue(predicate.test(this));
    }

    @Test public void testFunction() {
        
        SpringExpressionFunction<Object,SpringExpressionTest> func = new SpringExpressionFunction<>("#input");
        func.setOutputType(SpringExpressionTest.class);
        
        Assert.assertNull(func.apply(null));

        Assert.assertEquals(func.apply(this), this);

        Assert.assertEquals((int) new SpringExpressionFunction<>("#input.getValue99()").apply(this), 99);

    }

    /**
     * <p>Canary test for an issue reported to us as IDP-1901.</p>
     * 
     * <p>This is a bug in Spring Framework which we reported as GitHub issue 27995.</p>
     * 
     * <p>The upstream bug was fixed in Spring Framework 5.3.16; this is a canary test to
     * make sure we are aware if it reappears.</p>
     *
     * @throws Exception if the bad thing happens
     * 
     * @see <a href='https://shibboleth.atlassian.net/browse/IDP-1901'>IDP-1901</a>
     * @see <a href='https://github.com/spring-projects/spring-framework/issues/27995'>
     *   GitHub issue 27995</a>
     */
    @Test public void idp1901Canary() throws Exception {
        var map = new HashMap<String, String>();
        map.put("key", "value");
        var iter = map.entrySet().iterator();
        Assert.assertTrue(iter.hasNext());
        var output = new SpelExpressionParser().parseExpression("hasNext()").getValue(iter, Boolean.class);
        Assert.assertNotNull(output);
        Assert.assertTrue(output);
    }
}
