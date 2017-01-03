package com.groupon.jenkins.buildtype.install_packages;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class GitConfig extends GlobalConfiguration {
    public static final String DEFAULT_CLONE_URL_TEMPlATE = "https://<DOMAIN>/<ORG>/<REPO>.git";
    private String cloneUrlTemplate;


    public GitConfig() {
        load();
    }

    public static GitConfig get() {
        return GlobalConfiguration.all().get(GitConfig.class);
    }

    public String getCloneUrlTemplate() {
        return StringUtils.isEmpty(this.cloneUrlTemplate) ? DEFAULT_CLONE_URL_TEMPlATE : this.cloneUrlTemplate;
    }

    public void setCloneUrlTemplate(final String cloneUrlTemplate) {
        this.cloneUrlTemplate = cloneUrlTemplate;
    }

    @Override
    public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }
}
