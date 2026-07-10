# Configure A Reverse Proxy

Expose the plugin's loopback HTTP adapter through an existing HTTPS server. TLS
certificates and public ports remain owned by the web server, not Paper.

## Steps

1. Set `http.enabled: true` and keep `http.bind-address: 127.0.0.1`.
2. Choose an unused loopback port, such as `8765`.
3. Restart the plugin.
4. Proxy both paths to that loopback listener:
   `/.well-known/minecraft-setup-manager/manifest.json` and
   `/.well-known/minecraft-setup-manager/attestations`.
5. Allow only `GET` on the manifest path and `POST` on the attestation path.
6. Overwrite `X-Forwarded-For` with the connecting client address; never append
   an untrusted incoming value.
7. Set `http.trust-forwarded-for: true` after that header behavior is verified.
8. Buffer the complete request body before proxying it to Paper.
9. Limit attestation request bodies to 16 KiB and add an edge rate limit.
10. Set strict client-header, client-body, and upstream-response timeouts. The
    built-in adapter is intentionally small and loopback-only; the reverse proxy
    owns public connection deadlines.

## Verification

An HTTPS `GET` to the manifest path returns `200`, JSON, an `ETag`, and
`Cache-Control: no-cache`. The plugin itself must remain unreachable on a public
interface or port.
