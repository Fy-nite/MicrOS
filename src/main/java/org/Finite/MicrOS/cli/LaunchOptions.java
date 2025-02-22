package org.Finite.MicrOS.cli;

import com.beust.jcommander.Parameter;

public class LaunchOptions {
    @Parameter(names = {"--app", "-a"}, description = "Launch specific app by ID")
    private String appId;

    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help;

    public String getAppId() {
        return appId;
    }

    public boolean isHelp() {
        return help;
    }
}
