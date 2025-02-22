package org.Finite.MicrOS.cli;

import com.beust.jcommander.Parameter;

public class LaunchOptions {
    @Parameter(names = {"--app", "-a"}, description = "Launch specific app by ID")
    private String appId;

    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help;

    @Parameter(names = "--apppath", description = "Path to .app folder to launch")
    private String appPath;

    public String getAppId() {
        return appId;
    }

    public boolean isHelp() {
        return help;
    }

    public String getAppPath() {
        return appPath;
    }
}
