package com.lutzseverino.minecraftsetup.infrastructure.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lutzseverino.minecraftsetup.application.AttestationError;
import com.lutzseverino.minecraftsetup.application.AttestationRequest;
import com.lutzseverino.minecraftsetup.application.AttestationResult;
import com.lutzseverino.minecraftsetup.application.AttestationService;
import com.lutzseverino.minecraftsetup.application.ManifestSnapshotProvider;
import com.lutzseverino.minecraftsetup.application.ManifestUnavailableException;
import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import com.lutzseverino.minecraftsetup.domain.ProfileId;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LoopbackSetupHttpServer implements AutoCloseable {
  public static final String MANIFEST_PATH = "/.well-known/minecraft-setup-manager/manifest.json";
  public static final String ATTESTATION_PATH = "/.well-known/minecraft-setup-manager/attestations";
  private static final int MAX_REQUEST_BYTES = 16 * 1024;
  private static final ObjectMapper JSON =
      new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final HttpServer server;
  private final ExecutorService executor;
  private final ManifestSnapshotProvider manifests;
  private final AttestationService attestations;
  private final FailedAttemptRateLimiter rateLimiter;
  private final boolean trustForwardedFor;

  public LoopbackSetupHttpServer(
      String bindAddress,
      int port,
      boolean trustForwardedFor,
      ManifestSnapshotProvider manifests,
      AttestationService attestations,
      Clock clock)
      throws IOException {
    InetAddress address = InetAddress.getByName(bindAddress);
    if (!address.isLoopbackAddress()) {
      throw new IllegalArgumentException("The built-in HTTP adapter must bind to loopback");
    }
    this.manifests = Objects.requireNonNull(manifests, "manifests");
    this.attestations = Objects.requireNonNull(attestations, "attestations");
    this.trustForwardedFor = trustForwardedFor;
    this.rateLimiter = new FailedAttemptRateLimiter(clock, 120, Duration.ofMinutes(1), 4096);
    this.server = HttpServer.create(new InetSocketAddress(address, port), 16);
    this.executor =
        Executors.newFixedThreadPool(
            2,
            runnable -> {
              Thread thread = new Thread(runnable, "minecraft-setup-http");
              thread.setDaemon(true);
              return thread;
            });
    server.setExecutor(executor);
    server.createContext(MANIFEST_PATH, this::handleManifest);
    server.createContext(ATTESTATION_PATH, this::handleAttestation);
  }

  public void start() {
    server.start();
  }

  public int port() {
    return server.getAddress().getPort();
  }

  @Override
  public void close() {
    server.stop(1);
    executor.shutdownNow();
  }

  private void handleManifest(HttpExchange exchange) throws IOException {
    try (exchange) {
      if (!"GET".equals(exchange.getRequestMethod())) {
        sendProblem(exchange, 405, "method_not_allowed", "Use GET for this endpoint");
        return;
      }
      ManifestSnapshot snapshot;
      try {
        snapshot = manifests.current();
      } catch (ManifestUnavailableException exception) {
        sendProblem(exchange, 503, "manifest_unavailable", "The setup manifest is unavailable");
        return;
      }
      String etag = "\"" + snapshot.fingerprint().value() + "\"";
      if (etag.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
        exchange.getResponseHeaders().set("ETag", etag);
        exchange.sendResponseHeaders(304, -1);
        return;
      }
      exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
      exchange.getResponseHeaders().set("Cache-Control", "no-cache");
      exchange.getResponseHeaders().set("ETag", etag);
      send(exchange, 200, snapshot.publishedBytes());
    }
  }

  private void handleAttestation(HttpExchange exchange) throws IOException {
    try (exchange) {
      if (!"POST".equals(exchange.getRequestMethod())) {
        sendProblem(exchange, 405, "method_not_allowed", "Use POST for this endpoint");
        return;
      }
      String clientKey = clientKey(exchange);
      if (!rateLimiter.permitsAttempt(clientKey)) {
        sendProblem(exchange, 429, "rate_limited", "Too many setup code attempts");
        return;
      }
      String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
      if (contentType == null
          || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
        rateLimiter.recordFailure(clientKey);
        sendProblem(exchange, 400, "invalid_request", "Content-Type must be application/json");
        return;
      }

      byte[] bytes = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
      if (bytes.length > MAX_REQUEST_BYTES) {
        rateLimiter.recordFailure(clientKey);
        sendProblem(exchange, 400, "invalid_request", "Request body is too large");
        return;
      }

      AttestationRequest request;
      try {
        AttestationPayload payload = JSON.readValue(bytes, AttestationPayload.class);
        request = payload.toDomain();
      } catch (JsonProcessingException
          | IllegalArgumentException
          | NullPointerException exception) {
        rateLimiter.recordFailure(clientKey);
        sendProblem(exchange, 400, "invalid_request", "The setup request is invalid");
        return;
      }

      AttestationResult result = attestations.redeem(request);
      if (result.acceptedFingerprint().isPresent()) {
        sendJson(
            exchange,
            200,
            Map.of(
                "status",
                "accepted",
                "manifestFingerprint",
                result.acceptedFingerprint().orElseThrow().value()));
        return;
      }

      rateLimiter.recordFailure(clientKey);
      AttestationError error = result.error().orElseThrow();
      switch (error) {
        case CHALLENGE_INVALID ->
            sendProblem(exchange, 404, "challenge_invalid", "Setup code is not valid");
        case CHALLENGE_EXPIRED ->
            sendProblem(exchange, 410, "challenge_expired", "Setup code expired");
        case FINGERPRINT_MISMATCH ->
            sendProblem(exchange, 409, "fingerprint_mismatch", "Server setup changed");
        case PROFILE_INVALID ->
            sendProblem(exchange, 409, "profile_invalid", "Setup profile is not valid");
        case ATTESTATION_UNAVAILABLE ->
            sendProblem(exchange, 503, "attestation_unavailable", "Setup check is unavailable");
      }
    }
  }

  private static void sendProblem(HttpExchange exchange, int status, String code, String title)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "application/problem+json; charset=utf-8");
    send(
        exchange,
        status,
        JSON.writeValueAsBytes(
            Map.of(
                "type", "about:blank",
                "title", title,
                "status", status,
                "code", code)));
  }

  private String clientKey(HttpExchange exchange) {
    String remote = exchange.getRemoteAddress().getAddress().getHostAddress();
    if (!trustForwardedFor || !exchange.getRemoteAddress().getAddress().isLoopbackAddress()) {
      return remote;
    }
    String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
    if (forwarded == null) {
      return remote;
    }
    String candidate = forwarded.split(",", 2)[0].strip();
    if (candidate.length() < 2 || candidate.length() > 45 || !candidate.matches("[0-9A-Fa-f:.]+")) {
      return remote;
    }
    return candidate;
  }

  private static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    send(exchange, status, JSON.writeValueAsBytes(value));
  }

  private static void send(HttpExchange exchange, int status, byte[] body) throws IOException {
    exchange.sendResponseHeaders(status, body.length);
    exchange.getResponseBody().write(body);
  }

  private record ClientPayload(String name, String version) {}

  private record AttestationPayload(
      int protocolVersion,
      String challenge,
      String manifestFingerprint,
      String profileId,
      ClientPayload client) {
    private AttestationRequest toDomain() {
      Objects.requireNonNull(client, "client");
      return new AttestationRequest(
          protocolVersion,
          new ChallengeCode(challenge),
          new ManifestFingerprint(manifestFingerprint),
          new ProfileId(profileId),
          client.name(),
          client.version());
    }
  }
}
