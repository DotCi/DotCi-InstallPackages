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

import com.google.common.collect.Iterables;
import com.groupon.jenkins.buildtype.install_packages.buildconfiguration.configvalue.ConfigValue;
import com.groupon.jenkins.buildtype.util.shell.ShellCommands;
import hudson.matrix.Combination;

import java.util.Arrays;
import java.util.Collections;

public abstract class ConfigSection<T extends ConfigValue<?>> {
    private final String name;
    private final MergeStrategy mergeStrategy;
    protected T configValue;

    protected ConfigSection(String name, T configValue, MergeStrategy mergeStrategy) {
        this.name = name;
        this.mergeStrategy = mergeStrategy;
        this.configValue = configValue;
    }

    public abstract ShellCommands toScript(Combination combination);

    protected void merge(ConfigSection<T> otherConfigSection) {
        if (!otherConfigSection.getConfigValue().isEmpty()) {
            if (mergeStrategy.equals(MergeStrategy.REPLACE)) {
                configValue.replace(otherConfigSection.configValue);
            } else {
                configValue.append(otherConfigSection.configValue);
            }
        }
    }

    protected T getConfigValue() {
        return configValue;
    }

    protected void setConfigValue(T config) {
        this.configValue = config;
    }

    protected String getName() {
        return name;

    }

    public boolean isValid() {
        return Iterables.isEmpty(getValidationErrors());
    }

    public Iterable<String> getValidationErrors() {
        if (getConfigValue().isValid()) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(String.format("Invalid format for: %s", getName()));
        }
    }

    protected Object getFinalConfigValue() {
        return getConfigValue().getValue();
    }

    public boolean isSpecified() {
        return !getConfigValue().isEmpty();
    }

    protected enum MergeStrategy {
        REPLACE, APPEND
    }
}
