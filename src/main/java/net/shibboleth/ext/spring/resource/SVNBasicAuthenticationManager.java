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
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import org.springframework.beans.factory.InitializingBean;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;

/**
 * Authentication manager for SVN resources based on {@link BasicAuthenticationManager}. <br/>
 * Exbeds the proxy information into individual beans to allow setting from Spring.
 */
@ThreadSafe
public class SVNBasicAuthenticationManager extends BasicAuthenticationManager implements InitializingBean {

    /** Proxy Host - if needed. */
    private String proxyHost;

    /** Proxy Port - if needed. */
    private int proxyPort;

    /** Proxy User Name - if needed. */
    private String proxyUserName;

    /** Proxy Password - if needed. */
    private String proxyPassword;

    /** This is true if any of the setters for the proxy information have been called.*/
    private boolean proxySet;

    /**
     * Constructor. See http://svnkit.com/javadoc/org/tmatesoft/svn/core/auth/BasicAuthenticationManager.html
     * 
     * @param authentications authentications
     */
    public SVNBasicAuthenticationManager(final List<SVNAuthentication> authentications) {
        super(authentications.toArray(new SVNAuthentication[authentications.size()]));
    }

    /**
     * Constructor. See http://svnkit.com/javadoc/org/tmatesoft/svn/core/auth/BasicAuthenticationManager.html
     * 
     * @param userName username
     * @param keyFile a private key file
     * @param passphrase  a password to the private key
     * @param portNumber a port number over which an ssh tunnel is established
     */
    @SuppressWarnings("deprecation")
    public SVNBasicAuthenticationManager(final String userName, final File keyFile, 
            final String passphrase, final int portNumber) {
        super(userName, keyFile, passphrase, portNumber);
    }

    /**
     * Constructor. See http://svnkit.com/javadoc/org/tmatesoft/svn/core/auth/BasicAuthenticationManager.html
     *
     * @param userName a userName
     * @param password a password
     */
    @SuppressWarnings("deprecation")
    public SVNBasicAuthenticationManager(final String userName, final String password) {
        super(userName, password);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override public void afterPropertiesSet() {
        if (proxySet) {
            setProxy(proxyHost, proxyPort, proxyUserName, proxyPassword);
        }
    }

    /** Get the proxy host.
     * @return Returns the proxyHost.
     */
    @Override
    public String getProxyHost() {
        return proxyHost;
    }

    /** Set the proxy host.
     * @param host The proxyHost to set.
     */
    public void setProxyHost(final String host) {
        proxyHost = host;
        proxySet = true;
    }

    /** Get the proxy port.
     * @return Returns the proxyPort.
     */
    @Override
    public int getProxyPort() {
        return proxyPort;
    }

    /** Set the proxy port.
     * @param port The proxyPort to set.
     */
    public void setProxyPort(final int port) {
        proxyPort = port;
        proxySet = true;
    }

    /** Get the proxy user name.
     * @return Returns the proxyUserName.
     */
    @Override
    public String getProxyUserName() {
        return proxyUserName;
    }

    /** Set the proxy user name.
     * @param userName The proxyUserName to set.
     */
    public void setProxyUserName(final String userName) {
        proxyUserName = userName;
        proxySet = true;
    }

    /** Get the proxy password.
     * @return Returns the proxyPassword.
     */
    @Override
    public String getProxyPassword() {
        return proxyPassword;
    }

    /** Set the proxy password.
     * @param password The proxyPassword to set.
     */
    public void setProxyPassword(final String password) {
        proxyPassword = password;
        proxySet = true;
    }

}