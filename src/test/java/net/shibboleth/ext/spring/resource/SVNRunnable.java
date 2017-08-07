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

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;

/**
 * Implementation of a {@link Runnable} that communications with a Subversion server (via a spool directory).
 * 
 */
public class SVNRunnable extends AbstractIdentifiedInitializableComponent
        implements Runnable, BeanNameAware, InitializingBean {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SVNRunnable.class);

    /** SVN Client manager. */
    @Nonnull private final SVNClientManager clientManager;

    /** URL to the remote repository. */
    @Nonnull private SVNURL remoteRepository;

    /** Directory where the working copy will be kept. */
    @Nonnull private File workingCopyDirectory;

    /** Revision of the working copy. */
    private SVNRevision retrievalRevision;

    /**
     * Constructor.
     * 
     * @param svnClientMgr manager used to create SVN clients
     * @param repositoryUrl URL of the remote repository
     * @param workingCopy directory that will serve as the root of the local working copy
     * @param workingRevision revision of the resource to retrieve or -1 for HEAD revision
     * @param resourceFile file, within the working copy, represented by this resource
     * 
     * @throws BeanCreationException thrown if there is a problem initializing the SVN resource
     */
    public SVNRunnable(@Nonnull @ParameterName(name = "svnClientMgr") final SVNClientManager svnClientMgr,
            @Nonnull @ParameterName(name = "repositoryUrl") final SVNURL repositoryUrl,
            @Nonnull @ParameterName(name = "workingCopy") final File workingCopy,
            @ParameterName(name = "workingRevision") final long workingRevision) {

        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        if (svnClientMgr == null) {
            log.error("SVN client manager may not be null");
            throw new BeanCreationException("SVN client manager may not be null");
        }
        clientManager = svnClientMgr;

        if (repositoryUrl == null) {
            throw new BeanCreationException("SVN repository URL may not be null");
        }
        remoteRepository = repositoryUrl;

        try {
            checkWorkingCopyDirectory(workingCopy);
            workingCopyDirectory = workingCopy;
        } catch (final IOException e) {
            throw new BeanCreationException(e.getMessage());
        }

        if (workingRevision < 0) {
            this.retrievalRevision = SVNRevision.HEAD;
        } else {
            this.retrievalRevision = SVNRevision.create(workingRevision);
        }

        try {
            checkoutOrUpdate();
        } catch (final IOException e) {
            throw new BeanCreationException(e.getMessage());
        }
    }

    /** Helper function to generate the full path for injecting into URLs and URIs.
     * 
     * @return the directory path (from svn) pus the file.
     *
     *         protected String getFullPath() { final StringBuffer buffer = new
     *         StringBuffer(remoteRepository.getPath().length() + 1 + getFilename().length());
     *         buffer.append(remoteRepository.getPath()).append('/').append(getFilename()); return buffer.toString(); }
     * 
     *         /** Helper function to return a complex scheme - a conjunction of 'svn' and the underlying protocol.
     * 
     * @return 'svn+'protocol
     */
    protected String getProtocol() {
        final StringBuffer buffer = new StringBuffer(4 + remoteRepository.getProtocol().length());
        buffer.append("svn+").append(remoteRepository.getProtocol());
        return buffer.toString();
    }

    /**
     * Checks that the given file exists, or can be created, is a directory, and is read/writable by this process.
     * 
     * @param directory the directory to check
     * 
     * @throws IOException thrown if the file is invalid
     */
    protected void checkWorkingCopyDirectory(@Nonnull final File directory) throws IOException {
        if (directory == null) {
            log.error("SVN working copy directory cannot be null");
            throw new IOException("SVN working copy directory cannot be null");
        }

        if (!directory.exists()) {
            final boolean created = directory.mkdirs();
            if (!created) {
                final String msg = "SVN working copy directory " + directory.getAbsolutePath()
                        + " does not exist and could not be created";
                log.error(msg);
                throw new IOException(msg);
            }
        }

        if (!directory.isDirectory()) {
            final String msg = "SVN working copy location " + directory.getAbsolutePath() + " is not a directory";
            log.error(msg);
            throw new IOException(msg);
        }

        if (!directory.canRead()) {
            final String msg =
                    "SVN working copy directory " + directory.getAbsolutePath() + " cannot be read by this process";
            log.error(msg);
            throw new IOException(msg);
        }

        if (!directory.canWrite()) {
            final String msg = "SVN working copy directory " + directory.getAbsolutePath()
                    + " cannot be written to by this process";
            log.error(msg);
            throw new IOException(msg);
        }
    }

    /**
     * Checks out the resource specified by the {@link #remoteRepository} in to the working copy
     * {@link #workingCopyDirectory}. If the working copy is empty than an SVN checkout is performed if the working copy
     * already exists then an SVN update is performed.
     * 
     * @throws IOException thrown if there is a problem communicating with the remote repository, the revision does not
     *             exist, or the working copy is unusable
     */
    protected void checkoutOrUpdate() throws IOException {
        log.debug("checking out or updating working copy");

        if (!workingCopyDirectoryExists()) {
            log.debug("Working copy does not yet exist, checking it out to {}", workingCopyDirectory.getAbsolutePath());
            checkoutResourceDirectory();
        } else {
            if (retrievalRevision != SVNRevision.HEAD) {
                log.debug("Working copy exists and version is pegged at {}, no need to update",
                        retrievalRevision.toString());
                return;
            }
            log.debug("Working copy exists, updating to latest version.");
            updateResourceDirectory();
        }
    }

    /**
     * Checks to see if the working copy directory exists.
     * 
     * @return true if the working copy directory exists, false otherwise
     */
    private boolean workingCopyDirectoryExists() {
        final File svnMetadataDir = new File(workingCopyDirectory, ".svn");
        return svnMetadataDir.exists();
    }

    /**
     * Fetches the content from the SVN repository and creates the local working copy.
     * 
     * @return the revision of the fetched content
     * 
     * @throws IOException thrown if there is a problem checking out the content from the repository
     */
    private SVNRevision checkoutResourceDirectory() throws IOException {
        try {
            final long newRevision = clientManager.getUpdateClient().doCheckout(remoteRepository, workingCopyDirectory,
                    retrievalRevision, retrievalRevision, SVNDepth.INFINITY, true);
            log.debug("Checked out revision {} from remote repository {} and stored it in local working directory {}",
                    new Object[] {newRevision, remoteRepository.toDecodedString(),
                            workingCopyDirectory.getAbsolutePath(),});
            return SVNRevision.create(newRevision);
        } catch (final SVNException e) {
            final String errMsg = "Unable to check out revsion " + retrievalRevision.toString()
                    + " from remote repository " + remoteRepository.toDecodedString() + " to local working directory "
                    + workingCopyDirectory.getAbsolutePath();
            log.error(errMsg, e);
            throw new IOException(errMsg, e);
        }
    }

     /** Updates an existing local working copy from the repository.
     * 
     * @return the revision of the fetched content
     * 
     * @throws IOException thrown if there is a problem updating the working copy
     */
    private void updateResourceDirectory() throws IOException {
        try {
            final long newRevision = clientManager.getUpdateClient().doUpdate(workingCopyDirectory, retrievalRevision,
                    SVNDepth.INFINITY, true, true);
            log.debug("Updated local working directory {} to revision {} from remote repository {}", new Object[] {
                    workingCopyDirectory.getAbsolutePath(), newRevision, remoteRepository.toDecodedString(),});
        } catch (final SVNException e) {
            final String errMsg = "Unable to update working copy of resoure " + remoteRepository.toDecodedString()
                    + " in working copy " + workingCopyDirectory.getAbsolutePath() + " to revsion "
                    + retrievalRevision.toString();
            log.error(errMsg, e);
            throw new IOException(errMsg, e);
        }
    }

    @Override public void afterPropertiesSet() throws Exception {
        initialize();
    }

    /** {@inheritDoc} */
    @Override public void setBeanName(final String name) {
        // For some reason Spring will call this after initialization.
        if (!isInitialized()) {
            setId(name);
        }
    }

    /** {@inheritDoc} */
    @Override public void run() {
        try {
            checkoutOrUpdate();
        } catch (final IOException e) {
            log.error("Failed to update", e);
        }
    }

}
