package io.github.tamawish.rwr.placeholder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PlaceholderCacheTest {
    @Test
    void reusesSnapshotsUntilTtlExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-31T00:00:00Z"));
        CountingSource source = new CountingSource(sampleWorld());
        PlaceholderCache cache = new PlaceholderCache(clock, 1_500);
        cache.setSource(source);

        cache.refreshIfNeeded();
        cache.refreshIfNeeded();
        clock.advance(Duration.ofMillis(1_499));
        cache.refreshIfNeeded();
        assertThat(source.worldLoads.get()).isEqualTo(1);

        clock.advance(Duration.ofMillis(2));
        cache.refreshIfNeeded();
        assertThat(source.worldLoads.get()).isEqualTo(2);
    }

    @Test
    void eventsInvalidateSnapshotsAndStoreOverlays() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-31T00:00:00Z"));
        Instant scheduled = Instant.parse("2026-08-31T00:10:00Z");
        CountingSource source = new CountingSource(sampleWorld());
        PlaceholderCache cache = new PlaceholderCache(clock, 5_000);
        cache.setSource(source);

        cache.refreshIfNeeded();
        cache.recordWarning("resource", scheduled);
        cache.refreshIfNeeded();
        assertThat(source.worldLoads.get()).isEqualTo(2);
        assertThat(cache.hasCountdown("RESOURCE")).isTrue();
        assertThat(cache.countdownSeconds("resource")).isEqualTo(600);

        cache.recordOutcome("resource", LastOutcome.SUCCESS);
        assertThat(cache.hasCountdown("resource")).isFalse();
        assertThat(cache.lastOutcome("resource")).contains(LastOutcome.SUCCESS);
    }

    @Test
    void missingApiClearsOverlays() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-31T00:00:00Z"));
        PlaceholderCache cache = new PlaceholderCache(clock, 1_500);
        cache.setSource(new CountingSource(sampleWorld()));
        cache.recordWarning("resource", Instant.parse("2026-08-31T01:00:00Z"));
        cache.setSource(null);
        assertThat(cache.apiAvailable()).isFalse();
        assertThat(cache.hasCountdown("resource")).isFalse();
        assertThat(cache.knownWorldIds()).isEmpty();
    }

    @Test
    void providerFailureIsRetriedOnlyAfterTtlWhileServingLastGoodView() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-31T00:00:00Z"));
        AtomicInteger loads = new AtomicInteger();
        SnapshotSource source = new SnapshotSource() {
            @Override
            public List<CachedWorld> managedWorlds() {
                if (loads.incrementAndGet() > 1) {
                    throw new IllegalStateException("temporary provider failure");
                }
                return List.of(sampleWorld());
            }

            @Override
            public Optional<CachedStatus> resetStatus(String worldId) {
                return Optional.empty();
            }
        };
        PlaceholderCache cache = new PlaceholderCache(clock, 1_000);
        cache.setSource(source);

        assertThat(cache.knownWorldIds()).containsExactly("resource");
        clock.advance(Duration.ofSeconds(1));
        assertThat(cache.knownWorldIds()).containsExactly("resource");
        assertThat(loads).hasValue(2);

        assertThat(cache.knownWorldIds()).containsExactly("resource");
        assertThat(loads).hasValue(2);

        clock.advance(Duration.ofSeconds(1));
        assertThat(cache.knownWorldIds()).containsExactly("resource");
        assertThat(loads).hasValue(3);
    }

    private static CachedWorld sampleWorld() {
        return new CachedWorld("resource", "resource_world", "Resource", "MANAGED", true);
    }

    private static final class CountingSource implements SnapshotSource {
        private final CachedWorld world;
        private final AtomicInteger worldLoads = new AtomicInteger();

        private CountingSource(CachedWorld world) {
            this.world = world;
        }

        @Override
        public List<CachedWorld> managedWorlds() {
            worldLoads.incrementAndGet();
            return List.of(world);
        }

        @Override
        public Optional<CachedStatus> resetStatus(String worldId) {
            return Optional.of(new CachedStatus(world.id(), world.worldName(), "IDLE", "Idle", false));
        }
    }

    private static final class MutableClock implements InstantSource {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
