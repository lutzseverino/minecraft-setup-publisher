package com.lutzseverino.minecraftsetup.bootstrap;

import com.lutzseverino.minecraftsetup.config.SetupPublisherSettings;
import com.lutzseverino.minecraftsetup.application.LoginGateDecision;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import com.lutzseverino.minecraftsetup.platform.bukkit.BukkitSettingsReader;
import com.lutzseverino.minecraftsetup.platform.bukkit.SetupLoginListener;
import com.lutzseverino.minecraftsetup.platform.bukkit.SetupPublisherCommand;
import com.lutzseverino.minecraftsetup.platform.bukkit.SetupPublisherHost;
import java.io.File;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftSetupPublisherPlugin extends JavaPlugin implements SetupPublisherHost {
    private volatile PluginRuntime runtime;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledManifestIfMissing();
        try {
            runtime = buildRuntime();
        } catch (Exception exception) {
            getLogger().severe("Could not start Minecraft Setup Publisher: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new SetupLoginListener(this), this);
        PluginCommand command = Objects.requireNonNull(
                getCommand("setuppublisher"),
                "setuppublisher command is missing from plugin.yml"
        );
        SetupPublisherCommand executor = new SetupPublisherCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void onDisable() {
        PluginRuntime active = runtime;
        runtime = null;
        if (active != null) {
            active.close();
        }
    }

    private PluginRuntime runtime() {
        PluginRuntime active = runtime;
        if (active == null) {
            throw new IllegalStateException("Plugin runtime is unavailable");
        }
        return active;
    }

    public SetupPublisherSettings settings() {
        return runtime().settings();
    }

    public LoginGateDecision decideLogin(java.util.UUID playerId, boolean bypass) {
        return runtime().loginGate().decide(playerId, bypass);
    }

    public ManifestSnapshot validateManifest() throws Exception {
        return runtime().validateManifest();
    }

    public ManifestSnapshot publishManifest() throws Exception {
        return runtime().publishManifest();
    }

    public ManifestSnapshot currentManifest() throws Exception {
        return runtime().currentManifest();
    }

    @Override
    public java.util.logging.Logger logger() {
        return getLogger();
    }

    public synchronized void reloadRuntime() throws Exception {
        PluginRuntime previous = runtime();
        previous.close();
        reloadConfig();
        try {
            runtime = buildRuntime();
        } catch (Exception exception) {
            runtime = null;
            getServer().getPluginManager().disablePlugin(this);
            throw exception;
        }
    }

    private PluginRuntime buildRuntime() throws Exception {
        SetupPublisherSettings settings = new BukkitSettingsReader(this).read();
        return PluginRuntime.start(this, settings);
    }

    private void saveBundledManifestIfMissing() {
        File manifest = new File(getDataFolder(), "manifest.json");
        if (!manifest.exists()) {
            saveResource("manifest.json", false);
        }
    }
}
