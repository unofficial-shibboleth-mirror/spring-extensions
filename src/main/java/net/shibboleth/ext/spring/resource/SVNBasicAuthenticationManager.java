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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.net.ssl.TrustManager;

import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.ISVNProxyManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.io.SVNRepository;

/** Authentication manager for SVN resources. */
@ThreadSafe
public class SVNBasicAuthenticationManager extends AbstractIdentifiableInitializableComponent implements
        ISVNAuthenticationManager, BeanNameAware, InitializingBean {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SVNBasicAuthenticationManager.class);

    /** Network connection timeout in milliseconds. */
    private int connectionTimeout;

    /** Read operation timeout in milliseconds. */
    private int readTimeout;

    /** The SSL trust manager. */
    private TrustManager trustManager;

    /** User authentication mechanisms. */
    private Map<String, SVNAuthentication> authenticationMethods;

    /** HTTP proxy configuration. */
    private final BasicProxyManager proxyManager;

    /**
     * Constructor.
     * 
     * @param authnMethods user authentication methods
     */
    public SVNBasicAuthenticationManager(List<SVNAuthentication> authnMethods) {
        connectionTimeout = 5000;
        readTimeout = 10000;
        setAuthenticationMethods(authnMethods);
        proxyManager = null;
    }

    /**
     * Constructor.
     * 
     * @param authnMethods user authentication methods
     * @param proxyHost host name or IP address of the proxy server
     * @param proxyPort port of the proxy server
     * @param proxyUser username used to connect to the proxy server
     * @param proxyPassword password used to connect to the proxy server
     */
    public SVNBasicAuthenticationManager(List<SVNAuthentication> authnMethods, String proxyHost, int proxyPort,
            String proxyUser, String proxyPassword) {
        connectionTimeout = 5000;
        readTimeout = 10000;
        setAuthenticationMethods(authnMethods);
        proxyManager = new BasicProxyManager(proxyHost, proxyPort, proxyUser, proxyPassword);
    }

    /** {@inheritDoc} */
    @Override public void acknowledgeAuthentication(boolean authnAccepted, String authnKind, String authnRealm,
            SVNErrorMessage error, SVNAuthentication authnMethods) throws SVNException {
        if (authnAccepted) {
            log.trace("Successful authentication to SVN repository with {} credentials", authnKind);
        } else {
            log.trace("Unable to authenticate to SVN repository with {} credentials", authnKind);
        }
    }

    /** {@inheritDoc} */
    @Override public void acknowledgeTrustManager(TrustManager manager) {
        log.debug("HTTPS connection trusted by trust manager");
    }

    /** {@inheritDoc} */
    @Override public int getConnectTimeout(SVNRepository repository) {
        return connectionTimeout;
    }

    /**
     * Sets the network connection timeout in milliseconds. If a value of zero or less is given than the value
     * {@link Integer#MAX_VALUE} will be used.
     * 
     * @param timeout network connection timeout in milliseconds
     */
    public void setConnectionTimeout(int timeout) {
        if (timeout <= 0) {
            connectionTimeout = Integer.MAX_VALUE;
        } else {
            connectionTimeout = timeout;
        }
    }

    /** {@inheritDoc} */
    @Override @Nullable public SVNAuthentication getFirstAuthentication(String authnKind, String authnRealm,
            SVNURL repository) throws SVNException {
        return authenticationMethods.get(authnKind);
    }

    /** {@inheritDoc} */
    @Override @Nullable public SVNAuthentication getNextAuthentication(String authnKind, String authnRealm,
            SVNURL respository) throws SVNException {
        return null;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public ISVNProxyManager getProxyManager(SVNURL repository) throws SVNException {
        return proxyManager;
    }

    /** {@inheritDoc} */
    @Override public int getReadTimeout(SVNRepository repository) {
        return readTimeout;
    }

    /**
     * Sets the read operation timeout in milliseconds. If a value of zero or less is given than the value
     * {@link Integer#MAX_VALUE} will be used.
     * 
     * @param timeout network connection timeout in milliseconds
     */
    public void setReadTimeout(int timeout) {
        if (timeout <= 0) {
            readTimeout = Integer.MAX_VALUE;
        } else {
            readTimeout = timeout;
        }
    }

    /** {@inheritDoc} */
    @Override @Nullable public TrustManager getTrustManager(SVNURL respository) throws SVNException {
        return trustManager;
    }

    /**
     * Sets the trust manager used when negotiating SSL/TLS connections.
     * 
     * @param manager trust manager used when negotiating SSL/TLS connections
     */
    public void setTrustManager(TrustManager manager) {
        trustManager = manager;
    }

    /** {@inheritDoc} */
    @Override public boolean isAuthenticationForced() {
        return false;
    }

    /** This function is not implemented. {@inheritDoc} */
    @Override public void setAuthenticationProvider(ISVNAuthenticationProvider arg0) {
    }

    /**
     * Sets the user authentication methods.
     * 
     * @param authnMethods user authentication methods
     */
    private void setAuthenticationMethods(List<SVNAuthentication> authnMethods) {
        if (authnMethods == null || authnMethods.size() == 0) {
            authenticationMethods = Collections.emptyMap();
        } else {
            HashMap<String, SVNAuthentication> methods = new HashMap<String, SVNAuthentication>();
            for (SVNAuthentication method : authnMethods) {
                if (methods.containsKey(method.getKind())) {
                    log.warn("An authentication method of type " + method.getKind()
                            + " has already been set, only the first will be used");
                } else {
                    methods.put(method.getKind(), method);
                }
            }
            authenticationMethods = Collections.unmodifiableMap(methods);
        }
    }

    /** {@inheritDoc} */
    @Override public void afterPropertiesSet() throws Exception {
        initialize();
    }

    /** {@inheritDoc} */
    @Override public void setBeanName(String name) {
        setId(name);
    }

    /** Basic implementation of {@link ISVNProxyManager}. */
    private class BasicProxyManager implements ISVNProxyManager {

        /** Host name or IP address of the proxy. */
        private final String host;

        /** Port of the proxy. */
        private final int port;

        /** Username used to connect to the proxy. */
        private final String user;

        /** Password used to connect to the proxy. */
        private final String password;

        /**
         * Constructor.
         * 
         * @param theHost host name or IP address of the proxy server
         * @param thePort port of the proxy server
         * @param username username used to connect to the proxy server
         * @param pass password used to connect to the proxy server
         */
        public BasicProxyManager(String theHost, int thePort, String username, String pass) {
            this.host = StringSupport.trimOrNull(theHost);
            if (this.host == null) {
                throw new IllegalArgumentException("Proxy host may not be null or empty");
            }

            this.port = thePort;

            this.user = StringSupport.trimOrNull(username);
            this.password = StringSupport.trimOrNull(pass);
        }

        /** {@inheritDoc} */
        @Override public void acknowledgeProxyContext(boolean accepted, SVNErrorMessage error) {
            if (accepted) {
                log.trace("Connected to HTTP proxy " + host + ":" + port);
            }
            log.error("Unable to connect to HTTP proxy " + host + ":" + port + " recieved error:\n"
                    + error.getFullMessage());
        }

        /** {@inheritDoc} */
        @Override public String getProxyHost() {
            return host;
        }

        /** {@inheritDoc} */
        @Override public String getProxyPassword() {
            return password;
        }

        /** {@inheritDoc} */
        @Override public int getProxyPort() {
            return port;
        }

        /** {@inheritDoc} */
        @Override public String getProxyUserName() {
            return user;
        }
    }
}
