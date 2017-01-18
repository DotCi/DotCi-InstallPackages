/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014, Groupon, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.groupon.jenkins.buildtype.install_packages;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.groupon.jenkins.buildtype.InvalidBuildConfigurationException;
import com.groupon.jenkins.buildtype.install_packages.buildconfiguration.BuildConfiguration;
import com.groupon.jenkins.buildtype.install_packages.buildconfiguration.BuildConfigurationCalculator;
import com.groupon.jenkins.buildtype.plugins.DotCiPluginAdapter;
import com.groupon.jenkins.buildtype.util.shell.ShellCommands;
import com.groupon.jenkins.buildtype.util.shell.ShellScriptRunner;
import com.groupon.jenkins.dynamic.build.DotCiBuildInfoAction;
import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.dynamic.build.DynamicSubBuild;
import com.groupon.jenkins.dynamic.build.execution.BuildExecutionContext;
import com.groupon.jenkins.dynamic.build.execution.SubBuildRunner;
import com.groupon.jenkins.dynamic.build.execution.SubBuildScheduler;
import com.groupon.jenkins.dynamic.buildtype.BuildType;
import com.groupon.jenkins.notifications.PostBuildNotifier;
import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class InstallPackagesBuild extends BuildType implements SubBuildRunner {
    private static final Logger LOGGER = Logger.getLogger(InstallPackagesBuild.class.getName());
    private BuildConfiguration buildConfiguration;

    @Override
    public String getDescription() {
        return "Install Packages";
    }

    @Override
    public Result runBuild(final DynamicBuild dynamicBuild, final BuildExecutionContext buildExecutionContext, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        this.buildConfiguration = calculateBuildConfiguration(dynamicBuild, listener);
        addProcessedYamlToDotCiInfoAction(dynamicBuild, this.buildConfiguration.getConfiguration());
        if (this.buildConfiguration.isSkipped()) {
            dynamicBuild.skip();
            return Result.SUCCESS;
        }

        dynamicBuild.setAxisList(getAxisList(this.buildConfiguration));
        final Result result;
        if (this.buildConfiguration.isParallized()) {
            result = runMultiConfigbuildRunner(dynamicBuild, this.buildConfiguration, listener, launcher);
        } else {
            result = runSingleConfigBuild(dynamicBuild, new Combination(ImmutableMap.of("script", "main")), this.buildConfiguration, buildExecutionContext, listener, launcher);
        }
        runPlugins(dynamicBuild, this.buildConfiguration.getPlugins(), listener, launcher);
        runNotifiers(dynamicBuild, this.buildConfiguration, listener);
        return result;
    }

    private void addProcessedYamlToDotCiInfoAction(final Run run, final Map config) {
        final DotCiBuildInfoAction dotCiBuildInfoAction = run.getAction(DotCiBuildInfoAction.class);
        if (dotCiBuildInfoAction == null) {
            run.addAction(new DotCiBuildInfoAction(new Yaml().dump(config)));
        } else {
            dotCiBuildInfoAction.setBuildConfiguration(new Yaml().dump(config));
        }
    }


    private boolean runNotifiers(final DynamicBuild build, final BuildConfiguration buildConfiguration, final BuildListener listener) {
        boolean result = true;
        final List<PostBuildNotifier> notifiers = buildConfiguration.getNotifiers();
        for (final PostBuildNotifier notifier : notifiers) {
            result = result & notifier.perform(build, listener);
        }
        return result;
    }

    @Override
    public Result runSubBuild(final Combination combination, final BuildExecutionContext dynamicSubBuildExecution, final BuildListener listener) throws IOException, InterruptedException {
        return runBuildCombination(combination, dynamicSubBuildExecution, listener);
    }

    private Result runMultiConfigbuildRunner(final DynamicBuild dynamicBuild, final BuildConfiguration buildConfiguration, final BuildListener listener, final Launcher launcher) throws InterruptedException, IOException {
        final SubBuildScheduler subBuildScheduler = new SubBuildScheduler(dynamicBuild, this, new SubBuildScheduler.SubBuildFinishListener() {
            @Override
            public void runFinished(final DynamicSubBuild subBuild) throws IOException {
                for (final DotCiPluginAdapter plugin : buildConfiguration.getPlugins()) {
                    plugin.runFinished(subBuild, dynamicBuild, listener);
                }
            }
        });

        try {
            final Iterable<Combination> axisList = getAxisList(buildConfiguration).list();
            Result combinedResult = subBuildScheduler.runSubBuilds(getMainRunCombinations(axisList), listener);
            if (combinedResult.equals(Result.SUCCESS) && !Iterables.isEmpty(getPostBuildCombination(axisList))) {
                final Result runSubBuildResults = subBuildScheduler.runSubBuilds(getPostBuildCombination(axisList), listener);
                combinedResult = combinedResult.combine(runSubBuildResults);
            }
            dynamicBuild.setResult(combinedResult);
            return combinedResult;
        } finally {
            try {
                subBuildScheduler.cancelSubBuilds(listener.getLogger());
            } catch (final Exception e) {
                // There is nothing much we can do at this point
                LOGGER.log(Level.SEVERE, "Failed to cancel subbuilds", e);
            }
        }
    }

    private Result runSingleConfigBuild(final DynamicBuild dynamicBuild, final Combination combination, final BuildConfiguration buildConfiguration, final BuildExecutionContext buildExecutionContext, final BuildListener listener, final Launcher launcher) throws IOException, InterruptedException {
        return runBuildCombination(combination, buildExecutionContext, listener);
    }

    private void runPlugins(final DynamicBuild dynamicBuild, final List<DotCiPluginAdapter> plugins, final BuildListener listener, final Launcher launcher) {
        for (final DotCiPluginAdapter plugin : plugins) {
            plugin.perform(dynamicBuild, launcher, listener);
        }
    }

    private Result runBuildCombination(final Combination combination, final BuildExecutionContext buildExecutionContext, final BuildListener listener) throws IOException, InterruptedException {
        final ShellCommands mainBuildScript = this.buildConfiguration.toScript(combination);
        return new ShellScriptRunner(buildExecutionContext, listener).runScript(mainBuildScript);
    }


    private BuildConfiguration calculateBuildConfiguration(final DynamicBuild build, final BuildListener listener) throws IOException, InterruptedException, InvalidBuildConfigurationException {
        return new BuildConfigurationCalculator().calculateBuildConfiguration(build.getGithubRepoUrl(), build.getSha(), build.getEnvironmentWithChangeSet(listener));
    }


    private AxisList getAxisList(final BuildConfiguration buildConfiguration) {
        AxisList axisList = new AxisList(new Axis("script", "main"));
        if (buildConfiguration.isMultiLanguageVersions() && buildConfiguration.isMultiScript()) {
            axisList = new AxisList(new Axis("language_version", buildConfiguration.getLanguageVersions()), new Axis("script", buildConfiguration.getScriptKeys()));
        } else if (buildConfiguration.isMultiLanguageVersions()) {
            axisList = new AxisList(new Axis("language_version", buildConfiguration.getLanguageVersions()));
        } else if (buildConfiguration.isMultiScript()) {
            axisList = new AxisList(new Axis("script", buildConfiguration.getScriptKeys()));
        }
        return axisList;
    }

    public List<Combination> getPostBuildCombination(final Iterable<Combination> axisList) {
        for (final Combination combination : axisList) {
            if (isPostBuild(combination)) {
                return Arrays.asList(combination);
            }
        }
        return Collections.emptyList();
    }

    private boolean isPostBuild(final Combination combination) {
        return "post_build".equals(combination.get("script"));
    }

    public Iterable<Combination> getMainRunCombinations(final Iterable<Combination> axisList) {
        return Iterables.filter(axisList, new Predicate<Combination>() {
            @Override
            public boolean apply(final Combination combination) {
                return !isPostBuild(combination);
            }
        });
    }

}
