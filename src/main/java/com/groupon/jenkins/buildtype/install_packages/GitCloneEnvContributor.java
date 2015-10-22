package com.groupon.jenkins.buildtype.install_packages;

import com.groupon.jenkins.dynamic.build.*;
import com.groupon.jenkins.git.*;
import hudson.*;
import hudson.model.*;

import java.io.*;

@Extension
public class GitCloneEnvContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(Job job, EnvVars envs, TaskListener listener) throws IOException, InterruptedException {
        if(job instanceof DynamicProject) {
            DynamicProject dynamicJob = ((DynamicProject) job);
            GitUrl gitUrl = new GitUrl(dynamicJob.getGithubRepoUrl());
            envs.put("DOTCI_INSTALL_PACKAGES_GIT_CLONE_URL", getCloneUrl(gitUrl));
        }
    }
    private String getCloneUrl(GitUrl gitUrl) {
        String cloneTemplate = GitConfig.get().getCloneUrlTemplate();
        return gitUrl.applyTemplate(cloneTemplate);
    }
}
