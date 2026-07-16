package com.lutzseverino.minecraftsetup.bootstrap;

import com.lutzseverino.minecraftsetup.application.AttestationService;
import com.lutzseverino.minecraftsetup.application.ChallengeIssuer;
import com.lutzseverino.minecraftsetup.application.LoginGateService;
import com.lutzseverino.minecraftsetup.application.ManifestPublicationService;
import com.lutzseverino.minecraftsetup.config.SetupPublisherSettings;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import com.lutzseverino.minecraftsetup.domain.SetupCompliancePolicy;
import com.lutzseverino.minecraftsetup.infrastructure.http.LoopbackSetupHttpServer;
import com.lutzseverino.minecraftsetup.infrastructure.manifest.CachedManifestSnapshotProvider;
import com.lutzseverino.minecraftsetup.infrastructure.manifest.ProtocolManifestReader;
import com.lutzseverino.minecraftsetup.infrastructure.persistence.InMemoryChallengeRepository;
import com.lutzseverino.minecraftsetup.infrastructure.persistence.JsonComplianceRepository;
import com.lutzseverino.minecraftsetup.infrastructure.persistence.SecureChallengeCodeGenerator;
import com.lutzseverino.minecraftsetup.infrastructure.publisher.AtomicFileManifestPublisher;
import java.nio.file.Path;
import java.time.Clock;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

final class PluginRuntime implements AutoCloseable {
  private final SetupPublisherSettings settings;
  private final CachedManifestSnapshotProvider manifests;
  private final ManifestPublicationService publication;
  private final LoginGateService loginGate;
  private final LoopbackSetupHttpServer httpServer;

  private PluginRuntime(
      SetupPublisherSettings settings,
      CachedManifestSnapshotProvider manifests,
      ManifestPublicationService publication,
      LoginGateService loginGate,
      LoopbackSetupHttpServer httpServer) {
    this.settings = settings;
    this.manifests = manifests;
    this.publication = publication;
    this.loginGate = loginGate;
    this.httpServer = httpServer;
  }

  static PluginRuntime start(JavaPlugin plugin, SetupPublisherSettings settings) throws Exception {
    validateIdentityConfiguration(settings);
    Clock clock = Clock.systemUTC();
    CachedManifestSnapshotProvider manifests =
        new CachedManifestSnapshotProvider(new ProtocolManifestReader(settings.manifestSource()));
    ManifestPublicationService publication =
        new ManifestPublicationService(
            manifests, manifests, new AtomicFileManifestPublisher(settings.publicationOutput()));
    ManifestSnapshot loaded =
        settings.publishOnStart() ? publication.publish() : publication.activateWithoutPublishing();

    InMemoryChallengeRepository challenges = new InMemoryChallengeRepository(clock, 10_000);
    Path recordsPath = plugin.getDataFolder().toPath().resolve("state/compliance.json");
    JsonComplianceRepository compliance = new JsonComplianceRepository(recordsPath);
    ChallengeIssuer challengeIssuer =
        new ChallengeIssuer(
            challenges, new SecureChallengeCodeGenerator(), clock, settings.gate().challengeTtl());
    SetupCompliancePolicy policy =
        new SetupCompliancePolicy(settings.gate().mode(), settings.gate().failurePolicy());
    LoginGateService loginGate =
        new LoginGateService(policy, manifests, compliance, challengeIssuer);
    AttestationService attestations =
        new AttestationService(challenges, compliance, manifests, clock);

    LoopbackSetupHttpServer server = null;
    if (settings.http().enabled()) {
      server =
          new LoopbackSetupHttpServer(
              settings.http().bindAddress(),
              settings.http().port(),
              settings.http().trustForwardedFor(),
              manifests,
              attestations,
              clock);
      server.start();
    }

    plugin.getLogger().info("Loaded setup manifest " + loaded.fingerprint().value());
    return new PluginRuntime(settings, manifests, publication, loginGate, server);
  }

  SetupPublisherSettings settings() {
    return settings;
  }

  LoginGateService loginGate() {
    return loginGate;
  }

  ManifestSnapshot validateManifest() throws Exception {
    return publication.validate();
  }

  ManifestSnapshot publishManifest() throws Exception {
    return publication.publish();
  }

  ManifestSnapshot currentManifest() throws Exception {
    return manifests.current();
  }

  @Override
  public void close() {
    if (httpServer != null) {
      httpServer.close();
    }
  }

  private static void validateIdentityConfiguration(SetupPublisherSettings settings) {
    if (settings.gate().mode() == com.lutzseverino.minecraftsetup.domain.GateMode.ENFORCE
        && !Bukkit.getOnlineMode()
        && !settings.gate().identityForwardingTrusted()) {
      throw new IllegalArgumentException(
          "gate.mode=enforce requires online-mode or gate.identity-forwarding-trusted=true");
    }
    if (settings.gate().mode() == com.lutzseverino.minecraftsetup.domain.GateMode.ENFORCE
        && !settings.http().enabled()) {
      throw new IllegalArgumentException("gate.mode=enforce requires http.enabled=true");
    }
    if (settings.gate().mode() == com.lutzseverino.minecraftsetup.domain.GateMode.ENFORCE
        && !settings.http().trustForwardedFor()) {
      throw new IllegalArgumentException(
          "gate.mode=enforce requires http.trust-forwarded-for=true and a sanitizing reverse proxy");
    }
  }
}
