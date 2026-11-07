# Proposal

**Conventional Commits:** `feat(format): accept snapshot versions in release.json`

## Why

Content releases are published like every other OneLiteFeather artifact: releases to `onelitefeather-releases`, snapshots as `<version>-SNAPSHOT` to `onelitefeather-snapshots`. Format 1 only allows `X.Y.Z` and `X.Y.Z-dev.N` in `release.json`, so a snapshot data JAR would break the contract, and its version would not match the Maven version it was resolved under.

## What Changes

- `release.json` `version` is identical to the Maven version of the data JAR: `X.Y.Z` for releases, `X.Y.Z-SNAPSHOT` for snapshots.
- The `-dev.N` form is dropped. No producer has ever published it.
- A snapshot build is identified by its `revision`, not by its version.
- The format-1 schema in `vulpes-format` and the contract tests change accordingly; the format number stays 1.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `content-format`: The release version follows the Maven version, including `-SNAPSHOT`.

## Non-goals

- No other pre-release forms (`-rc.N`, `-beta`).
- No caching policy for snapshot dependencies; games set it in their own build.

## Impact

- The backend proposal `snapshot-format-1` publishes data JARs with these versions and links to the schema in `vulpes-format/src/main/resources/net/onelitefeather/vulpes/format/release-1.schema.json`.
- Generated `VERSION` constants can end with `-SNAPSHOT`; `REVISION` tells two snapshot builds apart.
