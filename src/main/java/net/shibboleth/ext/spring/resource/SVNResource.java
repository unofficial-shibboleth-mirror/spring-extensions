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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;

/**
 * Implementation of a Spring {@link Resource} that communications with a Subversion server (via a spool directory).
 * 
 * This resource will fetch the given resource as follows:
 * <ul>
 * <li>If the revision is a positive number the resource will fetch the resource once during construction time and will
 * never attempt to fetch it again.</li>
 * <li>If the revision number is zero or less, signaling the HEAD revision, every call this resource will cause the
 * resource to check to see if the current working copy is the same as the revision in the remote repository. If it is
 * not the new revision will be retrieved.</li>
 * </ul>
 * 
 * The behavior of multiple {@link SVNResource} operating on the same local copy are undefined.
 */
public class SVNResource extends AbstractIdentifiedInitializableComponent implements Resource, BeanNameAware,
        InitializingBean, net.shibboleth.utilities.java.support.resource.Resource {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SVNResource.class);

    /** SVN Client manager. */
    private final SVNClientManager clientManager;

    /** URL to the remote repository. */
    private SVNURL remoteRepository;

    /** Directory where the working copy will be kept. */
    private File workingCopyDirectory;

    /** Revision of the working copy. */
    private SVNRevision retrievalRevision;

    /** File, within the working copy, represented by this resource. */
    private String resourceFileName;

    /** Time the resource file was last modified. */
    private DateTime lastModified;

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
    public SVNResource(SVNClientManager svnClientMgr, SVNURL repositoryUrl, File workingCopy, long workingRevision,
            String resourceFile) {
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
        } catch (IOException e) {
            throw new BeanCreationException(e.getMessage());
        }

        if (workingRevision < 0) {
            this.retrievalRevision = SVNRevision.HEAD;
        } else {
            this.retrievalRevision = SVNRevision.create(workingRevision);
        }

        setFilename(resourceFile);

        try {
            checkoutOrUpdateResource();
            if (!getFile().exists()) {
                log.error("Resource file " + resourceFile + " does not exist in SVN working copy directory "
                        + workingCopy.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new BeanCreationException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public String getFilename() {
        return resourceFileName;
    }

    /**
     * Set the resourceFileName.
     * 
     * @param fileName the name
     */
    public void setFilename(String fileName) {
        resourceFileName = StringSupport.trimOrNull(fileName);
        if (resourceFileName == null) {
            log.error("SVN working copy resource file name may not be null or empty");
            throw new BeanCreationException("SVN working copy resource file name may not be null or empty");
        }
    }

    /**
     * Helper function to generate the full path for injecting into URLs and URIs.
     * 
     * @return the directory path (from svn) pus the file.
     */
    protected String getFullPath() {
        final StringBuffer buffer = new StringBuffer(remoteRepository.getPath().length() + 1 + getFilename().length());
        buffer.append(remoteRepository.getPath()).append('/').append(getFilename());
        return buffer.toString();
    }

    /**
     * Helper function to return a complex scheme - a conjunction of 'svn' and the underlying protocol.
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
    protected void checkWorkingCopyDirectory(File directory) throws IOException {
        if (directory == null) {
            log.error("SVN working copy directory cannot be null");
            throw new IOException("SVN working copy directory cannot be null");
        }

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
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
            final String msg = "SVN working copy directory " + directory.getAbsolutePath()
                    + " cannot be read by this process"; 
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
    protected void checkoutOrUpdateResource() throws IOException {
        log.debug("checking out or updating working copy");
        SVNRevision newRevision;

        if (!workingCopyDirectoryExists()) {
            log.debug("Working copy does not yet exist, checking it out to {}", workingCopyDirectory.getAbsolutePath());
            newRevision = checkoutResourceDirectory();
        } else {
            if (retrievalRevision != SVNRevision.HEAD) {
                lastModified = getLastModificationForRevision(SVNRevision.HEAD);
                log.debug("Working copy exists and version is pegged at {}, no need to update",
                        retrievalRevision.toString());
                return;
            }
            log.debug("Working copy exists, updating to latest version.");
            newRevision = updateResourceDirectory();
        }

        if (getFile().exists()) {
            log.debug("Determing last modification date of revision {}", newRevision.getNumber());
            lastModified = getLastModificationForRevision(newRevision);
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
            final long newRevision =
                    clientManager.getUpdateClient().doCheckout(remoteRepository, workingCopyDirectory,
                            retrievalRevision, retrievalRevision, SVNDepth.INFINITY, true);
            log.debug(
                    "Checked out revision {} from remote repository {} and stored it in local working directory {}",
                    new Object[] {newRevision, remoteRepository.toDecodedString(),
                            workingCopyDirectory.getAbsolutePath(),});
            return SVNRevision.create(newRevision);
        } catch (final SVNException e) {
            final String errMsg =
                    "Unable to check out revsion " + retrievalRevision.toString() + " from remote repository "
                            + remoteRepository.toDecodedString() + " to local working directory "
                            + workingCopyDirectory.getAbsolutePath();
            log.error(errMsg, e);
            throw new IOException(errMsg, e);
        }
    }

    /**
     * Gets the last modified time for the given revision.
     * 
     * @param revision revision to get the last modified date for
     * 
     * @return the last modified time
     * 
     * @throws IOException thrown if there is a problem getting the last modified time
     */
    private DateTime getLastModificationForRevision(SVNRevision revision) throws IOException {
        try {
            final SVNStatusHandler handler = new SVNStatusHandler();
            clientManager.getStatusClient().doStatus(getFile(), revision, SVNDepth.INFINITY, true, true, false, false,
                    handler, null);
            final SVNStatus status = handler.getStatus();

            // We want the date when this was committed
            return new DateTime(status.getCommittedDate());
        } catch (final SVNException e) {
            final String errMsg =
                    "Unable to check status of resource " + resourceFileName + " within working directory "
                            + workingCopyDirectory.getAbsolutePath();
            log.error(errMsg, e);
            throw new IOException(errMsg, e);
        }
    }

    /**
     * Updates an existing local working copy from the repository.
     * 
     * @return the revision of the fetched content
     * 
     * @throws IOException thrown if there is a problem updating the working copy
     */
    private SVNRevision updateResourceDirectory() throws IOException {
        try {
            final long newRevision =
                    clientManager.getUpdateClient().doUpdate(workingCopyDirectory, retrievalRevision,
                            SVNDepth.INFINITY, true, true);
            log.debug("Updated local working directory {} to revision {} from remote repository {}", new Object[] {
                    workingCopyDirectory.getAbsolutePath(), newRevision, remoteRepository.toDecodedString(),});
            return SVNRevision.create(newRevision);
        } catch (final SVNException e) {
            final String errMsg =
                    "Unable to update working copy of resoure " + remoteRepository.toDecodedString()
                            + " in working copy " + workingCopyDirectory.getAbsolutePath() + " to revsion "
                            + retrievalRevision.toString();
            log.error(errMsg, e);
            throw new IOException(errMsg, e);
        }
    }

    /** {@inheritDoc} */
    @Override public InputStream getInputStream() throws IOException {
        checkoutOrUpdateResource();
        try {
            return new FileInputStream(getFile());
        } catch (final IOException e) {
            log.error("Unable to read resource file {} from local working copy {}", resourceFileName,
                    workingCopyDirectory.getAbsolutePath(), e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override public boolean exists() {
        try {
            checkoutOrUpdateResource();
            return getFile().exists();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc} We always say yes.
     */
    @Override public boolean isReadable() {
        return exists();
    }

    /**
     * {@inheritDoc} Our stream can be read multiple times.
     */
    @Override public boolean isOpen() {
        return false;
    }

    /** {@inheritDoc} */
    @Override public URL getURL() throws IOException {

        return new URL(getProtocol(), remoteRepository.getHost(), remoteRepository.getPort(), getFullPath());
    }

    /** {@inheritDoc} */
    @Override public URI getURI() throws IOException {
        try {
            return new URI(getProtocol(), null, remoteRepository.getHost(), remoteRepository.getPort(), getFullPath(),
                    null, null);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public File getFile() throws IOException {
        return new File(workingCopyDirectory, resourceFileName);
    }

    /** {@inheritDoc} */
    @Override public long contentLength() throws IOException {
        return getFile().getTotalSpace();
    }

    /** {@inheritDoc} */
    @Override public long lastModified() throws IOException {
        checkoutOrUpdateResource();
        return lastModified.getMillis();
    }

    /** {@inheritDoc} */
    @Override public Resource createRelative(String relativePath) throws IOException {
        throw new IOException("Cannot support relative open on SVN resources");
    }

    /** {@inheritDoc} */
    @Override public net.shibboleth.utilities.java.support.resource.Resource
            createRelativeResource(String relativePath) throws IOException {
        throw new IOException("Cannot support relative open on SVN resources");
    }

    /** {@inheritDoc} */
    @Override public String getDescription() {
        StringBuffer sb = new StringBuffer("SVN Resource: ");
        return sb.append(getFullPath()).toString();
    }

    /** {@inheritDoc} */
    @Override public void afterPropertiesSet() throws Exception {
        initialize();
    }

    /** {@inheritDoc} */
    @Override public void setBeanName(String name) {
        // For some reason Spring will call this after initialization.
        if (!isInitialized()) {
            setId(name);
        }
    }

    /** Simple {@link ISVNStatusHandler} implementation that just stores and returns the status. */
    private class SVNStatusHandler implements ISVNStatusHandler {

        /** Current status of the resource. */
        private SVNStatus status;

        /**
         * Gets the current status of the resource.
         * 
         * @return current status of the resource
         */
        public SVNStatus getStatus() {
            return status;
        }

        /** {@inheritDoc} */
        @Override public void handleStatus(SVNStatus currentStatus) throws SVNException {
            status = currentStatus;
        }
    }

}
