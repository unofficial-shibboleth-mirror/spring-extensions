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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for the {@link SpringExpressionPredicate} and {@link SpringExpressionFunction}.
 */
public class SpringExpressionTest {

    /** Helper function to use this as the bean to test.
     * @return 99
     */
    public int getValue99() {
        return 99;
    }
     
    @Test public void testPredicates() {
        
        SpringExpressionPredicate predicate = new SpringExpressionPredicate<>("#input.getValue99() == 99");
        
        Assert.assertTrue(predicate.test(this));
        
        predicate = new SpringExpressionPredicate<>("#input.getValue99() == #custom");
        predicate.setCustomObject(99);
        Assert.assertTrue(predicate.test(this));

    }

    @Test public void testFunction() {
        
        SpringExpressionFunction<Object, SpringExpressionTest> func = new SpringExpressionFunction<>("#input");
        Assert.assertNull(func.apply(null));

        Assert.assertEquals(func.apply(this), this);

        Assert.assertEquals((int) new SpringExpressionFunction<SpringExpressionTest, Integer>("#input.getValue99()").apply(this), 99);

    }
}
