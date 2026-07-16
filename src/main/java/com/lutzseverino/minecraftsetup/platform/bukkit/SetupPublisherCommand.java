package com.lutzseverino.minecraftsetup.platform.bukkit;

import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class SetupPublisherCommand implements CommandExecutor, TabCompleter {
  private static final List<String> SUBCOMMANDS =
      List.of("status", "validate", "publish", "reload");
  private final SetupPublisherHost plugin;

  public SetupPublisherCommand(SetupPublisherHost plugin) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
  }

  @Override
  public boolean onCommand(
      CommandSender sender, Command command, String label, String[] arguments) {
    if (!sender.hasPermission("minecraftsetup.admin")) {
      sender.sendMessage("You do not have permission to manage client setup.");
      return true;
    }
    if (arguments.length != 1) {
      return false;
    }

    try {
      switch (arguments[0].toLowerCase(Locale.ROOT)) {
        case "status" -> showStatus(sender);
        case "validate" -> showManifest(sender, "Manifest is valid", plugin.validateManifest());
        case "publish" -> showManifest(sender, "Manifest published", plugin.publishManifest());
        case "reload" -> {
          plugin.reloadRuntime();
          sender.sendMessage("Minecraft Setup Publisher reloaded.");
        }
        default -> {
          return false;
        }
      }
    } catch (Exception exception) {
      sender.sendMessage(
          "Setup Publisher could not complete that action: " + exception.getMessage());
      plugin.logger().warning("Setup Publisher command failed: " + exception.getMessage());
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] arguments) {
    if (arguments.length != 1) {
      return List.of();
    }
    String prefix = arguments[0].toLowerCase(Locale.ROOT);
    return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
  }

  private void showStatus(CommandSender sender) throws Exception {
    ManifestSnapshot snapshot = plugin.currentManifest();
    sender.sendMessage("Manifest: " + snapshot.fingerprint().value());
    sender.sendMessage(
        "Gate mode: " + plugin.settings().gate().mode().name().toLowerCase(Locale.ROOT));
    sender.sendMessage(
        "HTTP adapter: " + (plugin.settings().http().enabled() ? "enabled" : "disabled"));
  }

  private static void showManifest(CommandSender sender, String prefix, ManifestSnapshot snapshot) {
    sender.sendMessage(prefix + ": " + snapshot.fingerprint().value());
  }
}
