package com.lutzseverino.minecraftsetup.platform.bukkit;

import com.lutzseverino.minecraftsetup.application.LoginGateDecision;
import com.lutzseverino.minecraftsetup.config.SetupPublisherSettings;
import com.lutzseverino.minecraftsetup.domain.GateOutcome;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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

        String message = messageFor(decision, settings.messages());
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text(message));
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
        event.getPlayer().sendMessage(Component.text(messageFor(decision, settings.messages())));
    }

    private static String messageFor(
            LoginGateDecision decision,
            SetupPublisherSettings.MessageSettings messages
    ) {
        String template = switch (decision.outcome()) {
            case DENY_SETUP_REQUIRED, ALLOW_ADVISORY_REQUIRED -> messages.setupRequired();
            case DENY_SETUP_OUTDATED, ALLOW_ADVISORY_OUTDATED -> messages.setupOutdated();
            case DENY_FAILURE -> messages.serviceUnavailable();
            default -> throw new IllegalArgumentException("Allowed login decision has no denial message");
        };
        String code = decision.challengeCode().map(value -> value.displayValue()).orElse("");
        return template.replace("{code}", code);
    }
}
