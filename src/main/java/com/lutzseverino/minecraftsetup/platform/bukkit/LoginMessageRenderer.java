package com.lutzseverino.minecraftsetup.platform.bukkit;

import com.lutzseverino.minecraftsetup.application.LoginGateDecision;
import com.lutzseverino.minecraftsetup.config.SetupPublisherSettings;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

final class LoginMessageRenderer {
    private static final String CONFIGURED_CODE_PLACEHOLDER = "{code}";
    private static final String MINIMESSAGE_CODE_PLACEHOLDER = "<minecraft_setup_challenge_code>";
    private static final String MINIMESSAGE_CODE_TAG = "minecraft_setup_challenge_code";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private LoginMessageRenderer() {
    }

    static Component render(
            LoginGateDecision decision,
            SetupPublisherSettings.MessageSettings messages
    ) {
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(messages, "messages");

        String template = switch (decision.outcome()) {
            case DENY_SETUP_REQUIRED, ALLOW_ADVISORY_REQUIRED -> messages.setupRequired();
            case DENY_SETUP_OUTDATED, ALLOW_ADVISORY_OUTDATED -> messages.setupOutdated();
            case DENY_FAILURE -> messages.serviceUnavailable();
            default -> throw new IllegalArgumentException("Login decision has no player-facing message");
        };
        String code = decision.challengeCode().map(value -> value.displayValue()).orElse("");
        return renderTemplate(template, code);
    }

    static Component renderTemplate(String template, String code) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(code, "code");

        String parsedTemplate = template.replace(
                CONFIGURED_CODE_PLACEHOLDER,
                MINIMESSAGE_CODE_PLACEHOLDER
        );
        return MINI_MESSAGE.deserialize(
                parsedTemplate,
                Placeholder.component(MINIMESSAGE_CODE_TAG, Component.text(code))
        );
    }
}
