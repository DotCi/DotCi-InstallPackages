/*
The MIT License (MIT)

Copyright (c) 2014, Groupon, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package com.groupon.jenkins.buildtype.install_packages.buildconfiguration;

import com.groupon.jenkins.buildtype.install_packages.buildconfiguration.configvalue.ListOrSingleValue;
import com.groupon.jenkins.buildtype.install_packages.buildconfiguration.configvalue.MapValue;
import com.groupon.jenkins.buildtype.util.shell.ShellCommands;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.String.format;

public class EnvironmentSection extends CompositeConfigSection {

    public static final String NAME = "environment";
    private final LanguageVersionsSection languageVersionsSection;
    private final LanguageSection languageSection;
    private final VarsSection varsSection;
    private final PackagesSection packagesSection;

    public EnvironmentSection(MapValue<String, ?> config) {
        super(NAME, config);
        languageVersionsSection = new LanguageVersionsSection(getSectionConfig(LanguageVersionsSection.NAME, ListOrSingleValue.class));
        languageSection = new LanguageSection(getSectionConfig(LanguageSection.NAME, com.groupon.jenkins.buildtype.install_packages.buildconfiguration.configvalue.StringValue.class));
        this.varsSection = new VarsSection(getSectionConfig(VarsSection.NAME, MapValue.class));

        packagesSection = new PackagesSection(getSectionConfig(PackagesSection.NAME, ListOrSingleValue.class), languageSection, languageVersionsSection);
        setSubSections(packagesSection, varsSection, languageSection, languageVersionsSection);
    }

    public boolean isMultiLanguageVersions() {
        return languageVersionsSection.isMultiLanguageVersions();
    }

    public List<String> getLanguageVersions() {
        return languageVersionsSection.getLanguageVersions();
    }

    public String getLanguage() {
        return languageSection.getLanguage();
    }

    public PackagesSection getPackagesSection() {
        return packagesSection;
    }



    public String buildCommandAmbassador(String buildCommand) {
        String shellPrefix = "sh -c \"env && ";
        if (buildCommand.contains(shellPrefix)) {
            int defaultEnvLength = shellPrefix.length();
            return new StringBuilder(buildCommand).insert(defaultEnvLength, DEFAULT_LINK_PROXY).toString();
        }
        return buildCommand;
    }

    // See the ambassador pattern :  https://docs.docker.com/articles/ambassador_pattern_linking/
    public final String DEFAULT_LINK_PROXY = "if [ -x /usr/bin/socat ]; then env | grep _TCP= | sed 's/.*_PORT_\\" +
            "([0-9]*\\)_TCP=tcp:\\/\\/\\(.*\\):\\(.*\\)/socat TCP4-LISTEN:\\1,fork,reuseaddr TCP4:\\2:\\3 \\&/' " +
            "| sh ;fi && ";
}