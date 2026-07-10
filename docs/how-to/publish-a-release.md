# Publish A Plugin Release

Use this guide to publish a verified plugin JAR and checksum from an immutable
version tag.

## Steps

1. Choose the next version according to the
   [Romantic Versioning policy](../reference/versioning.md) and update the
   `<version>` in `pom.xml`.
2. Review the complete change set and run the same build as CI:

   ```bash
   git submodule update --init
   ./mvnw --batch-mode --no-transfer-progress verify
   ```

3. Confirm the packaged JAR exists at
   `target/MinecraftSetupPublisher-PROJECT.MAJOR.MINOR.jar` and test it on a
   non-production Paper server. Exercise manifest validation, publication,
   discovery through HTTPS, and every enabled gate mode.
4. Merge the release commit to `main` and wait for CI to pass. The release tag
   must point at that reviewed commit; do not release from a local-only commit.
5. Create and push a tag that exactly matches `pom.xml`. For version `0.1.1`:

   ```bash
   git tag v0.1.1
   git push origin v0.1.1
   ```

   The tag workflow independently checks the tag/version match, runs the full
   Maven verification, creates a SHA-256 checksum, uploads both assets to a
   draft GitHub release, verifies the asset names, and then publishes it.
   Versions in project line `0` are marked as prereleases.

## Verification

1. Confirm the GitHub release is public and `v0.*` is marked **Pre-release**.
2. Confirm it contains exactly the expected versioned JAR and `.sha256` file.
3. Download both assets and verify the checksum:

   ```bash
   sha256sum --check MinecraftSetupPublisher-0.1.1.jar.sha256
   ```

4. Start a clean Paper test server with the downloaded JAR and confirm
   `/setuppublisher status` reports the expected runtime state.

## Notes

- Protect `v*` tag creation and changes to `.github/workflows/release.yml` with
  repository rules. Prefer a required reviewer on the `release` environment.
- Enable GitHub immutable releases before the first public release.
- A failed draft run can be retried. The workflow refuses to replace assets on
  an already-public release.
- Delete a mistaken unpublished tag and draft release instead of reusing a
  published version number.
