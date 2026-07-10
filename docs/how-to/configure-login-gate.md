# Configure The Login Gate

Require an exact current setup attestation before a player joins.

## Steps

1. Complete and verify the reverse-proxy setup.
2. Confirm the server uses `online-mode=true`, or confirm secure UUID forwarding
   and set `gate.identity-forwarding-trusted: true` deliberately.
3. Test with `gate.mode: advisory` before enforcement.
4. Adjust the three messages under `messages` using plain language. Keep the
   `{code}` placeholder in setup and update messages.
5. Confirm trusted staff have the configured bypass permission.
6. Set `gate.mode: enforce` and restart the plugin.

## Verification

A player without a current record sees a setup code before joining. Redeeming
that code after a successful local validation allows the next login. Publishing
a changed manifest causes the player to receive the update message.

## Notes

The default failure policy is `allow`, which avoids locking every player out
during a storage or manifest outage. Use `deny` only when that operational risk
is understood.
