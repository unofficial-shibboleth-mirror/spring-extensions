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

package net.shibboleth.ext.spring.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collections;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import com.google.common.io.Closeables;

/**
 * test,
 */
public class SVNResourceTest {

    static private final String ORIGINAL_TEXT = "This is a Test Resource which will be superseded by other Data";

    static private final int ORIGINAL_VERSION = 492;

    static private final String PATH = "data/";

    static private final String SVN_PATH = "/utilities/spring-extensions/trunk/src/test/resources/" + PATH;

    static private final String FILENAME = "TestResource.txt";

    static private final String CLASSPATH_PATH = PATH + FILENAME;

    private SVNClientManager clientManager;

    private Resource comparer = new ClassPathResource(CLASSPATH_PATH);

    private SVNURL url;

    private File theDir;

    @BeforeClass public void setup() throws SVNException, IOException {
        final ISVNAuthenticationManager authnManager = new SVNBasicAuthenticationManager(Collections.EMPTY_LIST);
        clientManager = SVNClientManager.newInstance();
        clientManager.setAuthenticationManager(authnManager);

        url = SVNURL.create("https", null, "svn.shibboleth.net", -1, SVN_PATH, false);

    }

    @BeforeMethod public void makeDir() throws IOException {
        Path p = Files.createTempDirectory("SVNResourceTest");
        theDir = p.toFile();
    }

    private void emptyDir(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                emptyDir(f);
            }
            f.delete();
        }
    }

    @AfterMethod public void emptyDir() {
        emptyDir(theDir);
        theDir.delete();
        theDir = null;
    }

    protected boolean compare(final Resource first, final Resource second) throws IOException {
        final InputStream firstStream = first.getInputStream();
        final InputStream secondStream = second.getInputStream();

        try {
            while (true) {
    
                final int firstInt = firstStream.read();
                final int secondInt = secondStream.read();
    
                if (firstInt == -1) {
                    return secondInt == -1;
                }
    
                if (firstInt != secondInt) {
                    return false;
                }
            }
        }
        finally {
            Closeables.close(firstStream, true);
            Closeables.close(secondStream, true);
        }
    }

    @Test public void testRevision() throws IOException, ParseException {
        final Resource resource = new SVNResource(clientManager, url, theDir, ORIGINAL_VERSION, FILENAME);
        Assert.assertTrue(resource.exists());

        Assert.assertEquals(resource.lastModified(),
                new DateTime(2013, 12, 31, 16, 54, 40, 927, DateTimeZone.UTC).getMillis());

        final Resource other = new ByteArrayResource(ORIGINAL_TEXT.getBytes());

        Assert.assertTrue(compare(other, resource));
        // Check rewind and check the compare code
        Assert.assertFalse(compare(comparer, resource));
        Assert.assertTrue(compare(other, resource));
    }

    @Test public void testNotExist() {
        final Resource resource = new SVNResource(clientManager, url, theDir, ORIGINAL_VERSION - 50, FILENAME);
        Assert.assertFalse(resource.exists());

    }

    @Test public void testMain() throws IOException {
        final Resource resource = new SVNResource(clientManager, url, theDir, -1, FILENAME);
        Assert.assertTrue(resource.exists());

        // CHANGE IF WE CHECKIN A NEW FILE
        long delta = resource.lastModified() - new DateTime(2013, 12, 31, 16, 59, 06, 500, DateTimeZone.UTC).getMillis();
        Assert.assertTrue(delta < 501 && delta > -501);

        Assert.assertTrue(compare(comparer, resource));

    }


}
