package com.lutzseverino.minecraftsetup.infrastructure.http;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

final class FailedAttemptRateLimiter {
  private final Clock clock;
  private final int limit;
  private final Duration window;
  private final int maximumClients;
  private final Map<String, Deque<Instant>> failures = new LinkedHashMap<>(16, 0.75f, true);

  FailedAttemptRateLimiter(Clock clock, int limit, Duration window, int maximumClients) {
    this.clock = clock;
    this.limit = limit;
    this.window = window;
    this.maximumClients = maximumClients;
  }

  synchronized boolean permitsAttempt(String clientKey) {
    Deque<Instant> clientFailures = failures.get(clientKey);
    if (clientFailures == null) {
      return true;
    }
    discardExpired(clientFailures);
    if (clientFailures.isEmpty()) {
      failures.remove(clientKey);
      return true;
    }
    return clientFailures.size() < limit;
  }

  synchronized void recordFailure(String clientKey) {
    Deque<Instant> clientFailures = failures.get(clientKey);
    if (clientFailures == null) {
      if (failures.size() >= maximumClients) {
        String leastRecentlyUsed = failures.keySet().iterator().next();
        failures.remove(leastRecentlyUsed);
      }
      clientFailures = new ArrayDeque<>();
      failures.put(clientKey, clientFailures);
    }
    discardExpired(clientFailures);
    clientFailures.addLast(clock.instant());
  }

  private void discardExpired(Deque<Instant> clientFailures) {
    Instant cutoff = clock.instant().minus(window);
    while (!clientFailures.isEmpty() && clientFailures.getFirst().isBefore(cutoff)) {
      clientFailures.removeFirst();
    }
  }
}
