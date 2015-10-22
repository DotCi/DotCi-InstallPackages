package com.groupon.jenkins.buildtype.install_packages;

import hudson.*;
import jenkins.model.*;
import net.sf.json.*;
import org.apache.commons.lang.*;
import org.kohsuke.stapler.*;

@Extension
public class GitConfig extends GlobalConfiguration {
    public static GitConfig get() {
        return GlobalConfiguration.all().get(GitConfig.class);
    }
    public static final String DEFAULT_CLONE_URL_TEMPlATE = "https://<DOMAIN>/<ORG>/<REPO>.git";


    public String getCloneUrlTemplate() {
        return StringUtils.isEmpty(cloneUrlTemplate)? DEFAULT_CLONE_URL_TEMPlATE: cloneUrlTemplate;
    }

    public void setCloneUrlTemplate(String cloneUrlTemplate) {
        this.cloneUrlTemplate = cloneUrlTemplate;
    }

    private String cloneUrlTemplate;

    public GitConfig() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }
}
