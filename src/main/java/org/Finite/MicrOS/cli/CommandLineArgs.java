package org.Finite.MicrOS.cli;

import com.beust.jcommander.Parameter;

public class CommandLineArgs {
    @Parameter(names = "--help", help = true, description = "Display this help message")
    private boolean help = false;

    @Parameter(names = "--version", description = "Display version information")
    private boolean version = false;

    @Parameter(names = "--init", description = "Initialize filesystem without starting the app")
    private boolean init = false;

    @Parameter(names = "--console", description = "Start in console-only mode")
    private boolean consoleOnly = false;

    @Parameter(names = "--debug", description = "Enable debug logging")
    private boolean debug = false;

    @Parameter(names = "--config", description = "Path to custom config file")
    private String configPath;

    @Parameter(names = "--fullscreen", description = "Start in fullscreen mode")
    private boolean fullscreen = false;

    public boolean isHelp() { return help; }
    public boolean isVersion() { return version; }
    public boolean isInit() { return init; }
    public boolean isConsoleOnly() { return consoleOnly; }
    public boolean isDebug() { return debug; }
    public String getConfigPath() { return configPath; }
    public boolean isFullscreen() { return fullscreen; }
}
