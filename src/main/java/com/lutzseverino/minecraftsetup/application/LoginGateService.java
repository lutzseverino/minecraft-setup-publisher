package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ComplianceRecord;
import com.lutzseverino.minecraftsetup.domain.GateOutcome;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import com.lutzseverino.minecraftsetup.domain.SetupChallenge;
import com.lutzseverino.minecraftsetup.domain.SetupCompliancePolicy;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class LoginGateService {
    private final SetupCompliancePolicy policy;
    private final ManifestSnapshotProvider manifests;
    private final ComplianceRepository compliance;
    private final ChallengeIssuer challenges;

    public LoginGateService(
            SetupCompliancePolicy policy,
            ManifestSnapshotProvider manifests,
            ComplianceRepository compliance,
            ChallengeIssuer challenges
    ) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.manifests = Objects.requireNonNull(manifests, "manifests");
        this.compliance = Objects.requireNonNull(compliance, "compliance");
        this.challenges = Objects.requireNonNull(challenges, "challenges");
    }

    public LoginGateDecision decide(UUID playerId, boolean bypass) {
        ManifestSnapshot manifest;
        Optional<ComplianceRecord> record;
        try {
            manifest = manifests.current();
            record = compliance.find(playerId);
        } catch (ManifestUnavailableException | RepositoryException exception) {
            GateOutcome outcome = policy.decideUnavailable(bypass);
            return outcome.denied()
                    ? LoginGateDecision.denied(outcome, Optional.empty())
                    : LoginGateDecision.allowed(outcome);
        }

        GateOutcome outcome = policy.decide(bypass, record, manifest.fingerprint());
        if (outcome == GateOutcome.ALLOW_ADVISORY_REQUIRED
                || outcome == GateOutcome.ALLOW_ADVISORY_OUTDATED) {
            SetupChallenge challenge = challenges.issue(playerId, manifest.fingerprint());
            return LoginGateDecision.advisory(outcome, challenge.code());
        }
        if (!outcome.denied()) {
            return LoginGateDecision.allowed(outcome);
        }
        if (outcome == GateOutcome.DENY_FAILURE) {
            return LoginGateDecision.denied(outcome, Optional.empty());
        }

        SetupChallenge challenge = challenges.issue(playerId, manifest.fingerprint());
        return LoginGateDecision.denied(outcome, Optional.of(challenge.code()));
    }
}
