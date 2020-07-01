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

package net.shibboleth.ext.spring.cli;

import java.io.PrintStream;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/** Command line arguments interface for the {@link CLI} command line tool. */
public interface CommandLineArguments {

    /**
     * Indicates the presence of the <code>--verbose</code> option.
     * 
     * @return <code>true</code> if the user requested verbose logging.
     */
    boolean isVerboseOutput();

    /**
     * Indicates the presence of the <code>--quiet</code> option.
     * 
     * @return <code>true</code> if the user requested quiet logging.
     */
    boolean isQuietOutput();

    /**
     * Gets the name of the requested logging configuration file
     * from the command line.
     * 
     * @return the logging configuration file name, or <code>null</code>.
     */
    @Nullable String getLoggingConfiguration();

    /**
     * Indicates the presence of the <code>--help</code> option.
     * 
     * @return <code>true</code> if the user requested help.
     */
    boolean isHelp();

    /**
     * Indicates the presence of the <code>--version</code> option.
     *
     * @return <code>true</code> if the user requested the version be printed.
     */
    boolean isVersion();

    /**
     * Get unparsed arguments.
     * 
     * @return unparsed arguments
     */
    @Nonnull @Unmodifiable @NotLive public List<String> getOtherArgs();

    /**
     * Validate the parameter set.
     * 
     * @throws IllegalArgumentException if the parameters are invalid
     */
    void validate() throws IllegalArgumentException;

    /**
     * Print default command line help instructions.
     * 
     * @param out location where to print the output
     */
    void printHelp(@Nonnull final PrintStream out);

}