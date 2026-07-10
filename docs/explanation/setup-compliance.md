# Setup Compliance

## Purpose

Define what the optional login gate can and cannot establish.

## Overview

When a player lacks a record for the current manifest fingerprint, the plugin
issues a random, short-lived code bound to the authenticated UUID and desired
state. The setup app redeems it only after local validation. A successful record
allows later login until the manifest changes.

## Key Concepts

- The app never submits a player name or UUID.
- Codes are single-use, expire quickly, and remain memory-only.
- Compliance records contain the UUID, fingerprint, profile, and validation time.
- Offline-mode identity is not trustworthy unless a proxy forwards UUIDs securely.

## Implications

This keeps ordinary players updated and gives server operators clear messages.
It is not anti-cheat: a public client request can be reproduced, and files may be
changed after validation. Continuous client proof would require a separate,
reviewed companion-mod handshake.
