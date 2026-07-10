# Configuration

`plugins/MinecraftSetupPublisher/config.yml` controls publication, the loopback
adapter, login policy, and player-facing messages.

| Key | Default | Meaning |
| --- | --- | --- |
| `manifest.source` | `manifest.json` | Plugin-owned source manifest |
| `manifest.publish-on-start` | `false` | Publish after successful startup validation |
| `publication.output` | `public/manifest.json` | Atomically written static output |
| `http.enabled` | `false` | Start the built-in loopback adapter |
| `http.bind-address` | `127.0.0.1` | Loopback address; non-loopback values are rejected |
| `http.port` | `8765` | Loopback listener port |
| `http.trust-forwarded-for` | `false` | Trust a proxy-overwritten client IP for per-player rate limits |
| `gate.mode` | `'off'` | `off`, `advisory`, or `enforce`; quote `off` in YAML |
| `gate.identity-forwarding-trusted` | `false` | Explicit trust for securely forwarded UUIDs |
| `gate.failure-policy` | `allow` | Login result when setup state is unavailable |
| `gate.challenge-ttl` | `10m` | Setup code lifetime, at most 15 minutes |
| `gate.bypass-permission` | `minecraftsetup.bypass` | Permission checked before gating |
| `messages.setup-required` | bundled text | First-setup denial message |
| `messages.setup-outdated` | bundled text | Update denial message |
| `messages.service-unavailable` | bundled text | Dependency-failure denial message |

Configured paths are relative to the plugin data directory. Absolute paths and
paths escaping that directory are rejected.

Player-facing messages support Paper's MiniMessage formatting. The
`messages.setup-required` and `messages.setup-outdated` values must retain the
literal `{code}` placeholder; it is replaced with the player's short-lived
setup code as plain text while the surrounding MiniMessage markup is rendered.
