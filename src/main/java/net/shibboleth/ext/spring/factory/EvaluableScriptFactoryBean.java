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

import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.io.Resource;

/**
 * A factory bean to summon up an {@link EvaluableScript} from either inline data or from a resource.
 */
public class EvaluableScriptFactoryBean extends AbstractComponentAwareFactoryBean<EvaluableScript> {

    /** log. */
    private final Logger log = LoggerFactory.getLogger(EvaluableScript.class);

    /** The resource which locates the script. */
    private Resource resource;

    /** The script. */
    private String script;

    /** The JSR223 engine name. */
    private String engineName;

    /** The source Id. */
    private String sourceId;

    /**
     * Get the resource which locates the script.
     * 
     * @return Returns the resource.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Set the resource which locates the script.
     * 
     * @param what the resource to set.
     */
    public void setResource(Resource what) {
        resource = what;
    }

    /**
     * Get the script.
     * 
     * @return Returns the script as text.
     */
    public String getScript() {
        return script;
    }

    /**
     * Set the script.
     * 
     * @param what the script to set.
     */
    public void setScript(String what) {
        script = what;
    }

    /**
     * Get the source ID.
     * 
     * @return Returns the sourceID.
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * Set the source Id.
     * 
     * @param what the Id to set.
     */
    public void setSourceId(String what) {
        sourceId = what;
    }

    /**
     * Get the engine Name.
     * 
     * @return Returns the engine name.
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Set the engine name.
     * 
     * @param what the engine name to set.
     */
    public void setEngineName(String what) {
        engineName = what;
    }

    /** {@inheritDoc} */
    @Override public Class<?> getObjectType() {
        return EvaluableScript.class;
    }

    /** {@inheritDoc} */
    @Override protected EvaluableScript doCreateInstance() throws Exception {

        if (null == script && null == resource) {
            log.error("{} A script or a resource must be supplied", sourceId);
            throw new BeanCreationException("A script or a resource must be supplied");
        }
        if (null != script && null != resource) {
            log.error("{} Only one of script or resource should be supplied", sourceId);
            throw new BeanCreationException("Only one of script or resource should be supplied");
        }

        if (null != resource) {
            script = StringSupport.inputStreamToString(resource.getInputStream(), null);
        }
        log.debug("{} Script: {}", sourceId, script);

        if (null == engineName) {
            log.debug("{} default language", sourceId);
            return new EvaluableScript(script);
        }
        log.debug("{} language : {}", sourceId, engineName);
        return new EvaluableScript(engineName, script);
    }

}
