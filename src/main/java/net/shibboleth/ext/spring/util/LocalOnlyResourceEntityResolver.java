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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Modified copy of Spring's existing {@link ResourceEntityResolver} class that
 * elides the fall-through logic allowing for http(s) resolution of entities.
 */
public class LocalOnlyResourceEntityResolver extends DelegatingEntityResolver {

	@Nonnull private final Logger log = LoggerFactory.getLogger(LocalOnlyResourceEntityResolver.class);

	@Nonnull private final ResourceLoader resourceLoader;

	/**
	 * Create a ResourceEntityResolver for the specified ResourceLoader
	 * (usually, an ApplicationContext).
	 * 
	 * @param loader the ResourceLoader (or ApplicationContext)
	 * to load XML entity includes with
	 */
	public LocalOnlyResourceEntityResolver(@Nonnull final ResourceLoader loader) {
		super(loader.getClassLoader());
		resourceLoader = loader;
	}

	/** {@inheritDoc} */
	@Override
	@Nullable public InputSource resolveEntity(@Nullable final String publicId, @Nullable final String systemId)
			throws SAXException, IOException {

		InputSource source = super.resolveEntity(publicId, systemId);

		if (source == null && systemId != null) {
			String resourcePath = null;
			try {
				String decodedSystemId = URLDecoder.decode(systemId, StandardCharsets.UTF_8);
				assert decodedSystemId != null;
				String givenUrl = new URL(decodedSystemId).toString();
				String systemRootUrl = new File("").toURI().toURL().toString();
				// Try relative to resource base if currently in system root.
				if (givenUrl.startsWith(systemRootUrl)) {
					resourcePath = givenUrl.substring(systemRootUrl.length());
				}
			}
			catch (Exception ex) {
				// Typically a MalformedURLException or AccessControlException.
				log.debug("Could not resolve XML entity [{}] against system root URL", systemId, ex);
				// No URL (or no resolvable URL) -> try relative to resource base.
				resourcePath = systemId;
			}
			if (resourcePath != null) {
				log.trace("Trying to locate XML entity [{}] as resource [{}]", systemId, resourcePath);
				Resource resource = this.resourceLoader.getResource(resourcePath);
				source = new InputSource(resource.getInputStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
				log.debug("Found XML entity [{}]:", systemId, resource);
			}
			else if (systemId.endsWith(DTD_SUFFIX) || systemId.endsWith(XSD_SUFFIX)) {
				// External dtd/xsd lookup via https even for canonical http declaration
				String url = systemId;
				if (url.startsWith("http:")) {
					url = "https:" + url.substring(5);
				}
				
				log.warn("Blocking attempted remote resolution of [{}]", systemId);
				// If we don't throw here, Java's broken parser just blindly proceeds with its own
				// internal entity resolution.
				throw new IOException("Blocked atttempted remote resolution");

                // This is being elided.
                
				/*
				try {
					source = new InputSource(ResourceUtils.toURL(url).openStream());
					source.setPublicId(publicId);
					source.setSystemId(systemId);
				}
				catch (IOException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve XML entity [" + systemId + "] through URL [" + url + "]", ex);
					}
					// Fall back to the parser's default behavior.
					source = null;
				}
				*/
			}
		}

		return source;
	}

}