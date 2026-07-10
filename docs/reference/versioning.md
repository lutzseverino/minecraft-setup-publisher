# Versioning

This repository follows
[Romantic Versioning](https://romversioning.github.io/romver/), written as
`PROJECT.MAJOR.MINOR`.

Project version `0` marks initial development. A major increment represents an
incompatible change within the same project, and a minor increment represents a
compatible feature or fix. A stable product moves the project number to `1.0.0`.

Release tags use the form `v0.1.0` and must exactly match `pom.xml`. The
tagged release workflow accepts exactly three numeric components, verifies the
project, publishes a versioned JAR plus SHA-256 checksum, and marks project line
`0` as prerelease. The independently consumed Minecraft Setup Protocol follows
Semantic Versioning and is versioned separately.

Follow [publish a plugin release](../how-to/publish-a-release.md) for the
operator procedure and post-release checks.
