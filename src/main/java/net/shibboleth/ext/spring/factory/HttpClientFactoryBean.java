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

package net.shibboleth.ext.spring.factory;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;

/**
 * Factory bean to accumulate the parameters into a {@link HttpClientBuilder} and to then emit a {@link HttpClient}.
 */
public class HttpClientFactoryBean extends AbstractComponentAwareFactoryBean<HttpClient> {

    /** Our captive builder. */
    private final HttpClientBuilder builder;
    
    /**
     * Connection timeout.<br/>
     * We need this field to ensure that Spring does the conversion.
     */
    @Duration private long connectionTimeout;
    
    /**
     * Connection request timeout.<br/>
     * We need this field to ensure that Spring does the conversion.
     */
    @Duration private long connectionRequestTimeout;

    /**
     * Socket timeout.<br/>
     * We need this field to ensure that Spring does the conversion.
     */
    @Duration private long socketTimeout;

    /**
     * Constructor.
     *
     */
    public HttpClientFactoryBean() {
        builder = createHttpClientBuilder();
    }

    /** {@inheritDoc} */
    @Override public Class<HttpClient> getObjectType() {

        return HttpClient.class;
    }
    
    /**
     * Sets the max total simultaneous connections allowed by the pooling connection manager.
     * 
     * @param max the max total connection
     */
    public void setMaxConnectionsTotal(final int max) {
        builder.setMaxConnectionsTotal(max);
    }
    
    /**
     * Sets the max simultaneous connections per route allowed by the pooling connection manager.
     * 
     * @param max the max connections per route
     */
    public void setMaxConnectionsPerRoute(final int max) {
        builder.setMaxConnectionsPerRoute(max);
    }

    /**
     * Sets the maximum length of time in milliseconds to wait for the connection to be established. A value of less
     * than 1 indicates no timeout.
     * 
     * @param timeout maximum length of time in milliseconds to wait for the connection to be established
     */
    @Duration public void setConnectionTimeout(@Duration final long timeout) {
        connectionTimeout = timeout;
        if (timeout > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout was too large");
        }
        builder.setConnectionTimeout((int) timeout);
    }
    
    /**
     * Sets the maximum length of time in milliseconds to wait for a connection to be returned from the connection
     * manager. A value of less than 1 indicates no timeout.
     * 
     * @param timeout maximum length of time in milliseconds to wait for a connection from the connection manager
     */
    @Duration public void setConnectionRequestTimeout(@Duration final long timeout) {
        connectionRequestTimeout = timeout;
        if (timeout > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout was too large");
        }
        builder.setConnectionRequestTimeout((int) timeout);
    }
    
    /**
     * Sets the maximum period inactivity between two consecutive data packets in milliseconds. A value of less
     * than 1 indicates no timeout.
     * 
     * @param timeout maximum length of time in milliseconds between two consecutive data packets
     */
    @Duration public void setSocketTimeout(@Duration final long timeout) {
        socketTimeout = timeout;
        if (timeout > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout was too large");
        }
        builder.setSocketTimeout((int) timeout);
    }
    
    /**
     * Set the TLS socket factory to use.
     * 
     * @param factory the new socket factory, may be null
     */
    public void setTLSSocketFactory(@Nullable final LayeredConnectionSocketFactory factory) {
        builder.setTLSSocketFactory(factory);
    }
    
    /**
     * Sets whether the responder's SSL/TLS certificate should be ignored.
     * 
     * @param disregard whether the responder's SSL/TLS certificate should be ignored
     */
    public void setConnectionDisregardTLSCertificate(final boolean disregard) {
        builder.setConnectionDisregardTLSCertificate(disregard);
    }

    /**
     * Sets the hostname of the default proxy used when making connection. A null indicates no default proxy.
     * 
     * @param host hostname of the default proxy used when making connection
     */
    public void setConnectionProxyHost(final String host) {
        builder.setConnectionProxyHost(host);
    }

    /**
     * Sets the port of the default proxy used when making connection.
     * 
     * @param port port of the default proxy used when making connection; must be greater than 0 and less than 65536
     */
    public void setConnectionProxyPort(final int port) {
        builder.setConnectionProxyPort(port);
    }

    /**
     * Sets the username to use when authenticating to the proxy.
     * 
     * @param usename username to use when authenticating to the proxy; may be null
     */
    public void setConnectionProxyUsername(final String usename) {
        builder.setConnectionProxyUsername(usename);
    }

    /**
     * Sets the password used when authenticating to the proxy.
     * 
     * @param password password used when authenticating to the proxy; may be null
     */
    public void setConnectionProxyPassword(final String password) {
        builder.setConnectionProxyPassword(password);
    }

    /**
     * Sets the user agent to be used when talking to the server. may not be null in which case the default will be
     * used.
     * 
     * @param agent what to set
     */
    public void setUserAgent(@Nullable final String agent) {
        builder.setUserAgent(agent);
    }
    
    /**
     * Create and return the instance of {@link HttpClientBuilder} to use.  
     * Subclasses may override to build a specialized subclass.
     * 
     * @return a new builder instance
     */
    protected HttpClientBuilder createHttpClientBuilder() {
        return new HttpClientBuilder();
    }
    
    /**
     * Get the instance of {@link HttpClientBuilder} to use.
     * 
     * @return the existing builder instance in use
     */
    protected HttpClientBuilder getHttpClientBuilder() {
        return builder;
    }

    /** {@inheritDoc} */
    @Override protected HttpClient doCreateInstance() throws Exception {
        return builder.buildClient();
    }
    
}
