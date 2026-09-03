# RWR-PlaceholderAPI

PlaceholderAPI expansion for [ResourceWorldResetter](https://github.com/TamaWish/ResourceWorldResetter).

**Artifact:** `io.github.tamawish:rwr-placeholderapi`  
**Package:** `io.github.tamawish.rwr.placeholder`  
**Repository:** [TamaWish/RWR-PlaceholderAPI](https://github.com/TamaWish/RWR-PlaceholderAPI)

## Features (v1.0)

- Soft-depends on both RWR runtimes (`ResourceWorldResetter` and `ResourceWorldResetter-Paper-Folia`) and PlaceholderAPI
- Uses only the public [RWR-API](https://github.com/TamaWish/RWR-API) service and reset events
- **Never shades `rwr-api`** — those classes come from the running RWR plugin
- Registers a single expansion with identifier `rwr` whenever PlaceholderAPI is present
- Returns configured fallbacks when `RwrApi` is missing or a world ID is unknown
- Caches `managedWorlds()` / `resetStatus()` for 1.5 seconds (configurable) so scoreboard ticks do not hammer the API
- Invalidates that cache on warning, pre-reset, and post-reset events
- Stores best-effort countdown / next-reset / last-outcome from events (lost on restart until the next event)
- Own locale system (`locale: en_US`, `locales/<code>.yml`, English fallback)
- Commands: `/rwrplaceholder reload` and `/rwrplaceholder status` (`rwrplaceholder.admin`, default op)

## Requirements

- Java 21+
- **Spigot**, **CraftBukkit**, **Paper**, **Purpur**, or **Folia** (Minecraft 1.21.4+)
- ResourceWorldResetter 5.1+ (the Spigot jar or the Paper/Folia jar that matches the server)
- PlaceholderAPI 2.11.7+

## Installation

1. Install ResourceWorldResetter for your server (**Spigot / CraftBukkit** → Spigot jar, **Paper / Purpur / Folia** → Paper-Folia jar) and PlaceholderAPI.
2. Drop `RWR-PlaceholderAPI-1.1.0.jar` into `plugins/`.
3. Start the server once to generate `plugins/RWR-PlaceholderAPI/config.yml`.
4. Use `%rwr_...%` placeholders in any PlaceholderAPI consumer (scoreboard, TAB, holograms, menus, chat).

The add-on stays loaded if RWR or PlaceholderAPI is missing. Placeholders return the configured fallback until both are available.

## Placeholders

Identifier: `rwr`

World IDs may contain underscores. `%rwr_world_resource_nether_phase%` is matched longest-first against known RWR IDs so it cannot steal `resource` from `resource_nether`.

### Global

| Placeholder | Meaning |
|-------------|---------|
| `%rwr_worlds%` | Comma-separated list of managed world IDs in RWR config order |
| `%rwr_world_names%` | Comma-separated list of managed world display names in RWR config order |

### Per world

Replace `<id>` with the RWR configuration ID (case-insensitive).

| Placeholder | Meaning | Source |
|-------------|---------|--------|
| `%rwr_world_<id>_id%` | Canonical RWR world ID | Snapshot |
| `%rwr_world_<id>_name%` | Administrator display name | Snapshot |
| `%rwr_world_<id>_world%` | Provider / Bukkit world name | Snapshot |
| `%rwr_world_<id>_state%` | Operational state (`managed`, `disabled`, `protected`, `orphaned`) | Snapshot |
| `%rwr_world_<id>_phase%` | Current reset phase, or `idle` | Snapshot |
| `%rwr_world_<id>_status%` | Human-readable diagnostic status line | Snapshot |
| `%rwr_world_<id>_can_reset%` | Whether the world is reset-capable | Snapshot (`resetCapable`) |
| `%rwr_world_<id>_resetting%` | Whether a reset is currently active | Snapshot |
| `%rwr_world_<id>_countdown%` | Whole seconds remaining until the last warned scheduled reset | Event cache |
| `%rwr_world_<id>_next_reset%` | Formatted UTC timestamp of that scheduled reset | Event cache |
| `%rwr_world_<id>_last_outcome%` | `success` / `failed` / `cancelled` / `interrupted` | Event cache |

### Player convenience

When the requesting player is online in a managed Bukkit world, the same keys work without a world ID:

`%rwr_id%`, `%rwr_name%`, `%rwr_world%`, `%rwr_state%`, `%rwr_phase%`, `%rwr_status%`, `%rwr_can_reset%`, `%rwr_resetting%`, `%rwr_countdown%`, `%rwr_next_reset%`, `%rwr_last_outcome%`

If the player is offline or not in a managed world, these return the fallback.

## Countdown and last-outcome limits

RWR-API 5.1.2 does **not** expose the scheduler, reset history, or last failure type on snapshots. This add-on therefore:

- Shows `%rwr_world_<id>_countdown%` and `%rwr_world_<id>_next_reset%` only after a `ResourceWorldResetWarningEvent` in this JVM
- Returns the fallback for those placeholders after a restart until the next warning fires
- Maps last outcome from `ResourceWorldPostResetEvent` (`COMPLETE` → success, `FAILED` + `EVENT_CANCELLED` → cancelled, other `FAILED` → failed, `INTERRUPTED` → interrupted)
- Clears the countdown when a post-reset event arrives for that world

This is correct 5.1.2 behaviour, not a bug.

## Configuration

```yaml
locale: en_US
cache-ttl-ms: 1500
datetime-format: "yyyy-MM-dd HH:mm:ss 'UTC'"
worlds-separator: ", "
boolean:
  true: "true"
  false: "false"
fallback: ""
no-api-fallback: ""
```

Locale files: `plugins/RWR-PlaceholderAPI/locales/en_US.yml` (bundled default). Missing keys fall back to the bundled English strings. State, phase, and outcome labels are locale keys (`state-MANAGED`, `phase-IDLE`, `outcome-cancelled`).

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/rwrplaceholder reload` | `rwrplaceholder.admin` | Reload config and locale |
| `/rwrplaceholder status` | `rwrplaceholder.admin` | Report RWR API, PlaceholderAPI, expansion, and cached world count |

Aliases: `/rwrpapi`, `/rwrph`

## Build

```shell
# Requires rwr-api 5.1.2 available to Maven (Central or local install)
mvn -f RWR-PlaceholderAPI/pom.xml verify
```

Compile-time dependencies are `rwr-api` (provided), `spigot-api` (provided), and `placeholderapi` (provided). None of them are shaded into the jar.

## License

BSD 3-Clause. See [LICENSE](LICENSE).
