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

## Capabilities

- validates strict protocol-v1 manifests and shared conformance fixtures;
- reproduces RFC 8785 canonical manifest fingerprints;
- publishes validated bytes with atomic file replacement;
- optionally serves the manifest and attestation endpoint on loopback for a
  reverse proxy;
- issues short-lived, single-use setup codes bound to a player UUID and exact
  manifest fingerprint;
- stores successful compliance records and invalidates them naturally when the
  manifest changes;
- blocks login with configurable ordinary-language messages when enforcement is
  explicitly enabled.

Setup attestation is workflow enforcement, not anti-cheat. A public desktop
client cannot prove that local files remain unchanged after validation.

## Development

The project targets Java 17 and Paper 1.20.1 or newer.

```bash
git submodule update --init
./mvnw verify
```

The packaged plugin is written to `target/MinecraftSetupPublisher-0.1.0.jar`.

## Documentation

Start with the [documentation index](docs/README.md). It includes a first setup
tutorial, configuration reference, reverse-proxy guidance, architecture, and the
login-gate trust boundary.

## Versioning

The plugin uses Romantic Versioning. See the
[versioning reference](docs/reference/versioning.md).

## License

Minecraft Setup Publisher is available under the [MIT License](LICENSE).
