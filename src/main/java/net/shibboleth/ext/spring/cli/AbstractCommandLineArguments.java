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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;

import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/** Command line arguments base class for the {@link AbstractCommandLine} class. */
public abstract class AbstractCommandLineArguments implements CommandLineArguments {

    // Non-option arguments
    
    /**
     * Command-line arguments which are not part of options.
     */
    @Parameter
    @Nonnull private List<String> otherArgs = new ArrayList<>();
    
    /**
     * Verbose logging has been requested.
     */
    @Parameter(names = "--verbose")
    private boolean verbose;

    /**
     * Quiet logging has been requested.
     */
    @Parameter(names = "--quiet")
    private boolean quiet;

    /**
     * Name of a specific logging configuration, if one has been requested.
     */
    @Parameter(names = "--logConfig")
    @Nullable private String logConfig;
    
    /**
     * Help has been requested.
     */
    @Parameter(names = "--help", help=true)
    private boolean help;

    /**
     * Version has been requested.
     */
    @Parameter(names = "--version")
    private boolean version;

    /** Spring property sources. */
    @Parameter(names = "--propertyFiles")
    @Nonnull private List<String> propertySources = new ArrayList<>();
    
    /** {@inheritDoc} */
    public boolean isVerboseOutput() {
        return verbose;
    }

    /** {@inheritDoc} */
    public boolean isQuietOutput() {
        return quiet;
    }

    /** {@inheritDoc} */
    @Nullable public String getLoggingConfiguration() {
        return logConfig;
    }

    /** {@inheritDoc} */
    public boolean isHelp() {
        return help;
    }

    /** {@inheritDoc} */
    public boolean isVersion() {
        return version;
    }
    
    /** {@inheritDoc} */
    @Nonnull @Unmodifiable @NotLive public List<String> getPropertyFiles() {
        return propertySources;
    }
    
    /** {@inheritDoc} */
    @Nonnull @Unmodifiable @NotLive public List<String> getOtherArgs() {
        return otherArgs;
    }

    /** {@inheritDoc} */
    public void validate() throws IllegalArgumentException {
        if (isVerboseOutput() && isQuietOutput()) {
            throw new IllegalArgumentException("Verbose and quiet output are mutually exclusive");
        }
    }

    /** {@inheritDoc} */
    public void printHelp(final PrintStream out) {
        out.println();
        out.println("==== Command Line Options ====");
        out.println();
        out.println(String.format("  --%-20s %s", "help", "Prints this help information"));
        out.println(String.format("  --%-20s %s", "version", "Prints version"));
        out.println(String.format("  --%-20s %s", "propertyFiles", "Comma-separated list of Spring property files"));
        out.println();

        out.println("Logging Options - these options are mutually exclusive");
        out.println(String.format("  --%-20s %s", "verbose", "Turn on verbose messages."));
        out.println(String.format("  --%-20s %s", "quiet", "Restrict output messages to errors and warnings."));
        out.println();
        out.println(String.format("  --%-20s %s", "logConfig",
                "Specifies a logback configuration file to use to configure logging."));
        out.println();
    }

}