package com.lutzseverino.minecraftsetup.platform.bukkit;

import com.lutzseverino.minecraftsetup.application.LoginGateDecision;
import com.lutzseverino.minecraftsetup.config.SetupPublisherSettings;
import com.lutzseverino.minecraftsetup.domain.GateOutcome;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public final class SetupLoginListener implements Listener {
  private final SetupPublisherHost plugin;

  public SetupLoginListener(SetupPublisherHost plugin) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onLogin(PlayerLoginEvent event) {
    if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
      return;
    }
    SetupPublisherSettings settings = plugin.settings();
    boolean bypass = event.getPlayer().hasPermission(settings.gate().bypassPermission());
    LoginGateDecision decision = plugin.decideLogin(event.getPlayer().getUniqueId(), bypass);
    if (!decision.outcome().denied()) {
      return;
    }

    event.disallow(
        PlayerLoginEvent.Result.KICK_OTHER,
        LoginMessageRenderer.render(decision, settings.messages()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJoin(PlayerJoinEvent event) {
    SetupPublisherSettings settings = plugin.settings();
    boolean bypass = event.getPlayer().hasPermission(settings.gate().bypassPermission());
    LoginGateDecision decision = plugin.decideLogin(event.getPlayer().getUniqueId(), bypass);
    if (decision.outcome() != GateOutcome.ALLOW_ADVISORY_REQUIRED
        && decision.outcome() != GateOutcome.ALLOW_ADVISORY_OUTDATED) {
      return;
    }
    event.getPlayer().sendMessage(LoginMessageRenderer.render(decision, settings.messages()));
  }
}
