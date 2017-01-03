package com.groupon.jenkins.buildtype.install_packages;

import com.groupon.jenkins.dynamic.build.DynamicProject;
import com.groupon.jenkins.git.GitUrl;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.TaskListener;

import java.io.IOException;

@Extension
public class GitCloneEnvContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(final Job job, final EnvVars envs, final TaskListener listener) throws IOException, InterruptedException {
        if (job instanceof DynamicProject) {
            final DynamicProject dynamicJob = ((DynamicProject) job);
            final GitUrl gitUrl = new GitUrl(dynamicJob.getGithubRepoUrl());
            envs.put("DOTCI_INSTALL_PACKAGES_GIT_CLONE_URL", getCloneUrl(gitUrl));
        }
    }

    private String getCloneUrl(final GitUrl gitUrl) {
        final String cloneTemplate = GitConfig.get().getCloneUrlTemplate();
        return gitUrl.applyTemplate(cloneTemplate);
    }
}
