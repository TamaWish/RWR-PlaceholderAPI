package io.github.tamawish.rwr.placeholder;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived snapshot cache plus best-effort event overlays.
 *
 * <p>World lists and reset status are refreshed lazily with a TTL. Warning and post-reset
 * events are stored until replaced or cleared. Reads are lock-light for scoreboard ticks.
 */
public final class PlaceholderCache {
    private final InstantSource clock;
    private volatile long ttlMs;
    private final Object lock = new Object();
    private final Map<String, Instant> scheduledResetAt = new ConcurrentHashMap<>();
    private final Map<String, LastOutcome> lastOutcomes = new ConcurrentHashMap<>();

    private volatile SnapshotSource source;
    private volatile WorldsView view = WorldsView.empty();

    public PlaceholderCache(InstantSource clock, long ttlMs) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttlMs = ttlMs;
    }

    public void setTtlMs(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public void setSource(SnapshotSource source) {
        this.source = source;
        invalidateSnapshots();
        if (source == null) {
            scheduledResetAt.clear();
            lastOutcomes.clear();
        }
    }

    public boolean apiAvailable() {
        return source != null;
    }

    public void invalidateSnapshots() {
        synchronized (lock) {
            view = WorldsView.empty();
        }
    }

    public WorldsView refreshIfNeeded() {
        SnapshotSource currentSource = source;
        if (currentSource == null) {
            return WorldsView.empty();
        }
        long now = clock.millis();
        WorldsView current = view;
        if (current.fetchedAt() > 0L && now - current.fetchedAt() < ttlMs) {
            return current;
        }
        synchronized (lock) {
            current = view;
            if (current.fetchedAt() > 0L && now - current.fetchedAt() < ttlMs) {
                return current;
            }
            WorldsView fresh = load(currentSource, now, current);
            view = fresh;
            return fresh;
        }
    }

    public List<String> knownWorldIds() {
        return refreshIfNeeded().ids();
    }

    public Optional<CachedWorld> world(String worldId) {
        return refreshIfNeeded().world(worldId);
    }

    public Optional<CachedWorld> worldByBukkitName(String worldName) {
        return refreshIfNeeded().worldByBukkitName(worldName);
    }

    public Optional<CachedStatus> status(String worldId) {
        return refreshIfNeeded().status(worldId);
    }

    public Optional<Instant> scheduledResetAt(String worldId) {
        return Optional.ofNullable(scheduledResetAt.get(normalize(worldId)));
    }

    public Optional<LastOutcome> lastOutcome(String worldId) {
        return Optional.ofNullable(lastOutcomes.get(normalize(worldId)));
    }

    public void recordWarning(String worldId, Instant scheduledAt) {
        Objects.requireNonNull(scheduledAt, "scheduledAt");
        scheduledResetAt.put(normalize(worldId), scheduledAt);
        invalidateSnapshots();
    }

    public void recordOutcome(String worldId, LastOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        String id = normalize(worldId);
        lastOutcomes.put(id, outcome);
        scheduledResetAt.remove(id);
        invalidateSnapshots();
    }

    public long countdownSeconds(String worldId) {
        Instant scheduled = scheduledResetAt.get(normalize(worldId));
        if (scheduled == null) {
            return -1L;
        }
        long seconds = Duration.between(clock.instant(), scheduled).getSeconds();
        return Math.max(0L, seconds);
    }

    public boolean hasCountdown(String worldId) {
        return scheduledResetAt.containsKey(normalize(worldId));
    }

    private WorldsView load(SnapshotSource currentSource, long now, WorldsView previous) {
        try {
            List<CachedWorld> worlds = List.copyOf(currentSource.managedWorlds());
            List<CachedStatus> statuses = new ArrayList<>();
            for (CachedWorld world : worlds) {
                currentSource.resetStatus(world.id()).ifPresent(statuses::add);
            }
            return new WorldsView(now, worlds, List.copyOf(statuses));
        } catch (RuntimeException ignored) {
            // Keep serving the last good data, but advance its timestamp so a temporary
            // provider failure cannot make every scoreboard/hologram request retry RWR.
            // The normal TTL becomes the retry interval; lifecycle events can still
            // invalidate this view immediately when the provider recovers or changes.
            return previous.withFetchedAt(now);
        }
    }

    private static String normalize(String worldId) {
        return Objects.requireNonNull(worldId, "worldId").trim().toLowerCase(Locale.ROOT);
    }

    /** Point-in-time world and status snapshot. */
    public static final class WorldsView {
        private final long fetchedAt;
        private final List<CachedWorld> worlds;
        private final List<CachedStatus> statuses;

        WorldsView(long fetchedAt, List<CachedWorld> worlds, List<CachedStatus> statuses) {
            this.fetchedAt = fetchedAt;
            this.worlds = worlds;
            this.statuses = statuses;
        }

        static WorldsView empty() {
            return new WorldsView(0L, List.of(), List.of());
        }

        WorldsView withFetchedAt(long fetchedAt) {
            return new WorldsView(fetchedAt, worlds, statuses);
        }

        long fetchedAt() {
            return fetchedAt;
        }

        public List<CachedWorld> worlds() {
            return worlds;
        }

        public List<String> ids() {
            return worlds.stream().map(CachedWorld::id).toList();
        }

        public Optional<CachedWorld> world(String worldId) {
            if (worldId == null || worldId.isBlank()) {
                return Optional.empty();
            }
            return worlds.stream().filter(world -> world.id().equalsIgnoreCase(worldId)).findFirst();
        }

        public Optional<CachedWorld> worldByBukkitName(String worldName) {
            if (worldName == null || worldName.isBlank()) {
                return Optional.empty();
            }
            return worlds.stream()
                    .filter(world -> world.worldName().equalsIgnoreCase(worldName))
                    .findFirst();
        }

        public Optional<CachedStatus> status(String worldId) {
            if (worldId == null || worldId.isBlank()) {
                return Optional.empty();
            }
            return statuses.stream()
                    .filter(status -> status.worldId().equalsIgnoreCase(worldId))
                    .findFirst();
        }
    }
}
