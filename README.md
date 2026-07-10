<div align="center">
  <h1>Minecraft Setup Publisher</h1>
  <p>Publish and enforce declarative Minecraft client setup requirements from Paper.</p>

  [![CI](https://github.com/lutzseverino/minecraft-setup-publisher/actions/workflows/ci.yml/badge.svg)](https://github.com/lutzseverino/minecraft-setup-publisher/actions/workflows/ci.yml)
  [![Releases](https://img.shields.io/github/v/release/lutzseverino/minecraft-setup-publisher?include_prereleases)](https://github.com/lutzseverino/minecraft-setup-publisher/releases)
  [![License: MIT](https://img.shields.io/badge/license-MIT-2f3437)](LICENSE)
</div>

Minecraft Setup Publisher is a Paper plugin for servers that use
[Minecraft Setup Manager](https://github.com/lutzseverino/minecraft-setup-manager).
It validates and publishes a declarative client setup manifest. It can also ask
players to apply the current setup before joining.

The plugin never sends scripts or inspects a player's computer. Manifests can
request only the bounded actions defined by the independently versioned
[Minecraft Setup Protocol](https://github.com/lutzseverino/minecraft-setup-protocol).

> [!IMPORTANT]
> Version `0.x` is under active development. Keep `gate.mode: off` until the
> matching Setup Manager attestation flow is installed and tested.

## How It Works

1. An operator writes a strict protocol-v1 manifest in the plugin data folder.
2. The plugin validates and fingerprints it before atomically publishing it.
3. A web server exposes that file at the standard HTTPS discovery path.
4. Minecraft Setup Manager reviews and applies the setup in an isolated game
   directory.
5. When the optional gate is enabled, the manager redeems a short-lived code
   after local validation and the plugin records the exact manifest fingerprint.

The built-in HTTP adapter binds to loopback and supplies the manifest and
attestation endpoints for a reverse proxy. Static publication is also supported
when only discovery is needed.

## Install

Download the plugin JAR from the
[GitHub Releases page](https://github.com/lutzseverino/minecraft-setup-publisher/releases),
place it in the Paper server's `plugins` directory, and restart the server. The
plugin targets Paper 1.20.1 or newer on Java 17 or newer.

Follow [publish your first setup](docs/tutorials/publish-your-first-setup.md) to
write, validate, and expose the manifest. Keep the login gate off until public
HTTPS discovery and attestation have both been tested.

## Player Flow

With `gate.mode: advisory`, players can join and receive a setup or update code
in chat. With `gate.mode: enforce`, a player without a current attestation sees
that code on the disconnect screen and must apply the setup before joining.

Codes are single-use, expire quickly, and are bound to the player's UUID and the
current manifest fingerprint. Publishing a changed manifest naturally makes an
older compliance record out of date. Trusted operators can hold the configured
`minecraftsetup.bypass` permission.

## Configuration

The generated `plugins/MinecraftSetupPublisher/config.yml` starts safely with
publication, HTTP, and login enforcement disabled:

```yaml
manifest:
  source: manifest.json
  publish-on-start: false
publication:
  output: public/manifest.json
http:
  enabled: false
  bind-address: 127.0.0.1
  port: 8765
  trust-forwarded-for: false
gate:
  mode: 'off' # off, advisory, or enforce
  failure-policy: allow
  challenge-ttl: 10m
messages:
  setup-required: '<yellow>Apply this server setup with code <bold>{code}</bold>.</yellow>'
```

Paths stay inside the plugin data directory. Player-facing text supports
MiniMessage; setup and update messages must retain the literal `{code}`
placeholder. Enforcement additionally requires the loopback HTTP adapter, a
sanitizing HTTPS reverse proxy, and trustworthy player UUIDs.

See the [configuration reference](docs/reference/configuration.md) for every
setting and its default.

## Commands

Administrators use `/setuppublisher status`, `/setuppublisher validate`,
`/setuppublisher publish`, and `/setuppublisher reload`. Validate a candidate
manifest before publishing it; publication is the operation that activates its
new fingerprint for login decisions.

See the
[commands and permissions reference](docs/reference/commands-and-permissions.md)
for command behavior and permission nodes.

## Game And Trust Boundaries

Setup attestation is workflow enforcement, not anti-cheat. A public desktop
client cannot prove that local files remain unchanged after validation.

The plugin never downloads mods or resource packs into the server and never
sends scripts to players. The manifest can describe only bounded protocol
actions. Server-side gameplay still depends on the manifest author choosing a
Minecraft version, loader, mods, resource packs, and configuration that work
together.

For stable player identity, use `online-mode=true`. A proxy deployment must
provide secure UUID forwarding before
`gate.identity-forwarding-trusted: true` is enabled. The default fail-open
policy keeps a manifest or storage outage from locking every player out; choose
fail-closed behavior only with that operational tradeoff understood.

## Development

The project targets Java 17 and Paper 1.20.1 or newer.

```bash
git submodule update --init
./mvnw verify
```

The packaged plugin is written to `target/MinecraftSetupPublisher-0.1.0.jar`.
Tagged releases follow Romantic Versioning; see
[publish a plugin release](docs/how-to/publish-a-release.md).

## Documentation

Start with the [documentation index](docs/README.md). Documentation is organized
by reader intent so durable guidance has one predictable home.

## License

Minecraft Setup Publisher is available under the [MIT License](LICENSE).
