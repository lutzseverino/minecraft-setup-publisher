# Publish Your First Setup

## Goal

Install the plugin, validate a server setup manifest, and publish it for a web
server or reverse proxy to expose over HTTPS.

## Prerequisites

- A Paper server running Java 17 or newer.
- A built plugin JAR.
- An HTTPS web server or reverse proxy for public discovery.

## Steps

1. Put the plugin JAR in the Paper server's `plugins` directory.
2. Start and stop Paper once to create `plugins/MinecraftSetupPublisher`.
3. Replace the example `manifest.json` with the setup your server requires.
4. Start Paper and run `/setuppublisher validate` as an operator.
5. Run `/setuppublisher publish`.
6. Expose `plugins/MinecraftSetupPublisher/public/manifest.json` at
   `/.well-known/minecraft-setup-manager/manifest.json` over public HTTPS.
7. Enter the Minecraft server address in Minecraft Setup Manager and review the
   published setup.

## Result

The validation command prints a fingerprint, the published file exists, and the
desktop app can discover the same manifest from the server address.
