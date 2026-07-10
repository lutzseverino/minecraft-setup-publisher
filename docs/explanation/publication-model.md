# Publication Model

## Purpose

Explain why the plugin writes a static file and binds its optional HTTP server to
loopback.

## Overview

Paper does not normally own public HTTPS certificates or port 443. The plugin
therefore validates an administrator-owned source, then writes a complete output
atomically. A web server can serve that file directly. When attestation is
needed, the same web server proxies the two well-known paths to the loopback
adapter.

## Key Concepts

- Public TLS remains an infrastructure concern.
- Clients never observe a partially written manifest.
- The built-in server rejects non-loopback binds.
- The manifest and attestation exchange share one HTTPS origin.

## Implications

The plugin works with Nginx, Caddy, Apache, a hosting panel, or static deployment
without teaching the domain about any of them.
