package com.lutzseverino.minecraftsetup.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SetupCompliancePolicyTest {
    private static final ManifestFingerprint CURRENT = fingerprint('a');
    private static final ManifestFingerprint OLD = fingerprint('b');

    @Test
    void enforcementRequiresMissingOrOutdatedPlayersToSetUp() {
        SetupCompliancePolicy policy = new SetupCompliancePolicy(GateMode.ENFORCE, FailurePolicy.ALLOW);

        assertEquals(
                GateOutcome.DENY_SETUP_REQUIRED,
                policy.decide(false, Optional.empty(), CURRENT)
        );
        assertEquals(
                GateOutcome.DENY_SETUP_OUTDATED,
                policy.decide(false, Optional.of(record(OLD)), CURRENT)
        );
        assertEquals(
                GateOutcome.ALLOW_CURRENT,
                policy.decide(false, Optional.of(record(CURRENT)), CURRENT)
        );
    }

    @Test
    void operationalFailureDefaultsCanKeepServerAvailable() {
        SetupCompliancePolicy failOpen = new SetupCompliancePolicy(GateMode.ENFORCE, FailurePolicy.ALLOW);
        SetupCompliancePolicy failClosed = new SetupCompliancePolicy(GateMode.ENFORCE, FailurePolicy.DENY);

        assertEquals(GateOutcome.ALLOW_FAILURE, failOpen.decideUnavailable(false));
        assertEquals(GateOutcome.DENY_FAILURE, failClosed.decideUnavailable(false));
        assertEquals(GateOutcome.ALLOW_BYPASS, failClosed.decideUnavailable(true));
    }

    @Test
    void advisoryModeDistinguishesFirstSetupFromAnUpdate() {
        SetupCompliancePolicy policy = new SetupCompliancePolicy(GateMode.ADVISORY, FailurePolicy.ALLOW);

        assertEquals(
                GateOutcome.ALLOW_ADVISORY_REQUIRED,
                policy.decide(false, Optional.empty(), CURRENT)
        );
        assertEquals(
                GateOutcome.ALLOW_ADVISORY_OUTDATED,
                policy.decide(false, Optional.of(record(OLD)), CURRENT)
        );
    }

    private static ComplianceRecord record(ManifestFingerprint fingerprint) {
        return new ComplianceRecord(UUID.randomUUID(), fingerprint, new ProfileId("standard"), Instant.EPOCH);
    }

    private static ManifestFingerprint fingerprint(char character) {
        return new ManifestFingerprint("msm-v1-sha256:" + String.valueOf(character).repeat(64));
    }
}
