# Architecture

## Purpose

Keep manifest policy, login decisions, storage, HTTP, and Paper lifecycle code in
their correct owners so each can change independently.

## Overview

The domain contains immutable values and pure compliance policy. Application
services coordinate ports for manifests, challenges, records, and publication.
Infrastructure implements JSON, files, canonicalization, and loopback HTTP.
The Bukkit package translates commands and login events. Bootstrap performs
wiring and lifecycle management.

## Key Concepts

- Dependencies point inward from adapters to application and domain.
- The pinned protocol submodule owns the wire contract and conformance fixtures.
- Bukkit handlers contain translation, not manifest or persistence policy.
- Challenge and compliance repositories are synchronized across Paper and HTTP
  threads.
- Publication occurs only after complete structural and semantic validation.
- Candidate validation is read-only. A new fingerprint becomes active for login
  policy only after publication succeeds.

## Implications

The static publisher, HTTP adapter, and login gate can be used independently.
Future Modrinth resolution or signing belongs in infrastructure behind existing
application ports, not in commands or event listeners.
