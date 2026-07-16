package com.lutzseverino.minecraftsetup.platform.bukkit;

import com.lutzseverino.minecraftsetup.config.DurationParser;
import com.lutzseverino.minecraftsetup.config.SetupPublisherSettings;
import com.lutzseverino.minecraftsetup.domain.FailurePolicy;
import com.lutzseverino.minecraftsetup.domain.GateMode;
import java.nio.file.Path;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitSettingsReader {
  private final JavaPlugin plugin;

  public BukkitSettingsReader(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public SetupPublisherSettings read() {
    FileConfiguration config = plugin.getConfig();
    Path dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
    Path source = ownedPath(dataDirectory, required(config, "manifest.source"));
    Path output = ownedPath(dataDirectory, required(config, "publication.output"));

    SetupPublisherSettings.HttpSettings http =
        new SetupPublisherSettings.HttpSettings(
            config.getBoolean("http.enabled", false),
            required(config, "http.bind-address"),
            config.getInt("http.port", 8765),
            config.getBoolean("http.trust-forwarded-for", false));
    SetupPublisherSettings.GateSettings gate =
        new SetupPublisherSettings.GateSettings(
            gateMode(required(config, "gate.mode")),
            config.getBoolean("gate.identity-forwarding-trusted", false),
            enumValue(FailurePolicy.class, required(config, "gate.failure-policy")),
            DurationParser.parse(required(config, "gate.challenge-ttl")),
            required(config, "gate.bypass-permission"));
    SetupPublisherSettings.MessageSettings messages =
        new SetupPublisherSettings.MessageSettings(
            required(config, "messages.setup-required"),
            required(config, "messages.setup-outdated"),
            required(config, "messages.service-unavailable"));
    return new SetupPublisherSettings(
        source,
        config.getBoolean("manifest.publish-on-start", false),
        output,
        http,
        gate,
        messages);
  }

  private static Path ownedPath(Path root, String configuredPath) {
    Path configured = Path.of(configuredPath);
    if (configured.isAbsolute()) {
      throw new IllegalArgumentException("Configured files must stay in the plugin data folder");
    }
    Path resolved = root.resolve(configured).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("Configured files must stay in the plugin data folder");
    }
    return resolved;
  }

  private static String required(FileConfiguration config, String path) {
    String value = config.getString(path);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing config value: " + path);
    }
    return value;
  }

  private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
    try {
      return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Unsupported " + type.getSimpleName() + ": " + value, exception);
    }
  }

  private static GateMode gateMode(String value) {
    if (value.equalsIgnoreCase("false")) {
      return GateMode.OFF;
    }
    return enumValue(GateMode.class, value);
  }
}
