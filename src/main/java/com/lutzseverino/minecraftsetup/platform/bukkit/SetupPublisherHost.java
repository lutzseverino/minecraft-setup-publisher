package com.lutzseverino.minecraftsetup.platform.bukkit;

import com.lutzseverino.minecraftsetup.application.LoginGateDecision;
import com.lutzseverino.minecraftsetup.config.SetupPublisherSettings;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import java.util.UUID;
import java.util.logging.Logger;

public interface SetupPublisherHost {
    SetupPublisherSettings settings();

    LoginGateDecision decideLogin(UUID playerId, boolean bypass);

    ManifestSnapshot validateManifest() throws Exception;

    ManifestSnapshot publishManifest() throws Exception;

    ManifestSnapshot currentManifest() throws Exception;

    void reloadRuntime() throws Exception;

    Logger logger();
}
