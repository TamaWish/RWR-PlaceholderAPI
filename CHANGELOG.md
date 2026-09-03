# Changelog

Public **v1** history for RWR-PlaceholderAPI. All 1.x.x versions stay in this file. When v2 begins, start `CHANGELOG_v2.md`.

## 1.1.0 - 2026-09-03

- Added `%rwr_world_names%` for an automatically updated, config-ordered list of managed-world display names.

## 1.0.0 - 2026-08-31

- Added a PlaceholderAPI expansion with identifier `rwr` for ResourceWorldResetter 5.1.2 snapshots.
- Added per-world and player-world placeholders for ID, display name, Bukkit world name, state, phase, status, reset capability, and in-progress state.
- Added best-effort countdown, next-reset time, and last-outcome placeholders sourced from warning and post-reset events (cleared on restart until the next event).
- Added a 1.5s lazy snapshot cache with immediate invalidation on warning, pre-reset, and post-reset.
- Added longest-first world-ID parsing so IDs that contain underscores are not truncated.
- Added degraded operation when RWR or PlaceholderAPI is unavailable, Folia support, and `/rwrplaceholder reload|status`.
- Added TTL-based retry backoff for temporary RWR snapshot failures while continuing to serve the last good cached data.
- Kept snapshot placeholders available when only the optional reset-event overlay listener cannot be registered.
- Configured Mockito as an explicit test JVM agent for forward compatibility with Java 21+ agent-loading restrictions.

This add-on does not shade `rwr-api`.

Live smoke testing completed September 1, 2026 on Spigot 26.2 with ResourceWorldResetter 5.1.0,
PlaceholderAPI 2.12.3, TAB 6.1.2, and DecentHolograms 2.10.1. Direct PlaceholderAPI and TAB
parsing returned the configured world identity, managed/idle state, and reset booleans. A live
DecentHolograms dashboard refreshed through a successful reset, reported the completed verification
status, cleared its countdown, and changed the last outcome to `success`.
