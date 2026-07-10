# Commands And Permissions

## Command

`/setuppublisher` requires `minecraftsetup.admin`.

| Subcommand | Behavior |
| --- | --- |
| `status` | Shows the current fingerprint, gate mode, and HTTP state |
| `validate` | Reloads and validates the source manifest |
| `publish` | Reloads, validates, and atomically publishes the manifest |
| `reload` | Reloads configuration and rebuilds plugin services |

## Permissions

| Permission | Default | Behavior |
| --- | --- | --- |
| `minecraftsetup.admin` | Operators | Uses administration commands |
| `minecraftsetup.bypass` | Operators | Bypasses setup compliance gating |
